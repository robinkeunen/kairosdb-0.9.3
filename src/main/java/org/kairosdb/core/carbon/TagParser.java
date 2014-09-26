/*
 * Copyright 2013 Proofpoint Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kairosdb.core.carbon;

import org.kairosdb.core.DataPointSet;

import java.util.Map;

/**
 Created with IntelliJ IDEA.
 User: bhawkins
 Date: 10/1/13
 Time: 1:27 PM
 To change this template use File | Settings | File Templates.
 */
public interface TagParser
{
	/**
	 Parse the incomming metric name and return a DataPointSet to represent the
	 data.  The DataPointSet needs to contain the metric name and at least
	 one tag.  If a null is returned the metric is not submitted to the datastore
	 @param metricName
	 @return DataPointSet containing metric name and at least one tag or null.
	 */
	public DataPointSet parseMetricName(String metricName);
}
