/*
 * Copyright 2013 Proofpoint Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.kairosdb.core.reporting;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.kairosdb.core.DataPoint;
import org.kairosdb.core.DataPointSet;
import org.kairosdb.core.datastore.KairosDatastore;
import org.kairosdb.core.scheduler.KairosDBJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.kairosdb.util.Preconditions.checkNotNullOrEmpty;
import static org.quartz.TriggerBuilder.newTrigger;

public class MetricReporterService implements KairosDBJob
{
	public static final Logger logger = LoggerFactory.getLogger(MetricReporterService.class);

	public static final String HOSTNAME = "HOSTNAME";
	public static final String SCHEDULE_PROPERTY = "kairosdb.reporter.schedule";

	private KairosDatastore m_datastore;
	private List<KairosMetricReporter> m_reporters;
	private final String m_hostname;
	private final String m_schedule;


	@Inject
	public MetricReporterService(KairosDatastore datastore,
			List<KairosMetricReporter> reporters,
			@Named(SCHEDULE_PROPERTY) String schedule,
			@Named(HOSTNAME) String hostname)
	{
		m_datastore = checkNotNull(datastore);
		m_hostname = checkNotNullOrEmpty(hostname);
		m_reporters = reporters;
		m_schedule = schedule;
	}


	private int getThreadCount()
	{
		ThreadGroup tg = Thread.currentThread().getThreadGroup();
		while (tg.getParent() != null)
		{
			tg = tg.getParent();
		}

		return tg.activeCount();
	}

	@Override
	public Trigger getTrigger()
	{
		return (newTrigger()
				.withIdentity(this.getClass().getSimpleName())
				.withSchedule(CronScheduleBuilder.cronSchedule(m_schedule)) //Schedule to run every minute
				.build());
	}

	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException
	{
		logger.debug("Reporting metrics");
		long timestamp = System.currentTimeMillis();
		try
		{
			for (KairosMetricReporter reporter : m_reporters)
			{
				List<DataPointSet> dpList = reporter.getMetrics(timestamp);
				for (DataPointSet dataPointSet : dpList)
				{
					m_datastore.putDataPoints(dataPointSet);
				}
			}


			Runtime runtime = Runtime.getRuntime();
			Map<String, String> tags = new HashMap<String, String>();
			tags.put("host", m_hostname);
			m_datastore.putDataPoints(new DataPointSet("kairosdb.jvm.free_memory",
					tags, Collections.singletonList(new DataPoint(timestamp, runtime.freeMemory()))));
			m_datastore.putDataPoints(new DataPointSet("kairosdb.jvm.total_memory",
					tags, Collections.singletonList(new DataPoint(timestamp, runtime.totalMemory()))));
			m_datastore.putDataPoints(new DataPointSet("kairosdb.jvm.max_memory",
					tags, Collections.singletonList(new DataPoint(timestamp, runtime.maxMemory()))));
			m_datastore.putDataPoints(new DataPointSet("kairosdb.jvm.thread_count",
					tags, Collections.singletonList(new DataPoint(timestamp, getThreadCount()))));
		}
		catch (Throwable e)
		{
			// prevent the thread from dying
			logger.error("Reporter service error", e);
		}
	}
}