/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.grails.core.plugins.filters;

import java.util.List;

import org.apache.grails.core.plugins.PluginMetadata;

/**
 * Defines interface for getting a sublist of <code>PluginMetadata</code> instances
 * based on an original supplied list of <code>PluginMetadata</code> instances.
 */
public interface PluginFilter {

    /**
     * Returns a filtered list of plugins.
     * @param original the original supplied set of <code>GrailsPlugin</code> instances
     * @return a sublist of these items
     */
    List<PluginMetadata> filterPluginList(List<PluginMetadata> original);
}
