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
package org.apache.grails.core.plugins;

import org.springframework.core.io.Resource;

/**
 * Lightweight container class holding {@link org.apache.grails.core.plugins.PluginMetadata} needed for ordering and
 * associated configuration resource.
 */
public final class PluginInfo {

    private final PluginMetadata metadata;
    private final PluginDescriptor pluginDescriptor;
    private final Resource configResource;
    private final boolean dynamic;

    /**
     * Creates a new {@code PluginInfo}.
     *
     * @param metadata the plugin metadata from {@link org.apache.grails.core.plugins.PluginDiscovery}
     * @param configResource the plugin's configuration resource ({@code plugin.yml} or
     *        {@code plugin.groovy}), or {@code null} if no config file exists
     */
    PluginInfo(PluginDescriptor pluginDescriptor, PluginMetadata metadata, Resource configResource, boolean dynamic) {
        this.metadata = metadata;
        this.pluginDescriptor = pluginDescriptor;
        this.configResource = configResource;
        this.dynamic = dynamic;
    }

    public String getName() {
        return metadata.getName();
    }

    public String getPluginVersion() {
        return metadata.getPluginVersion();
    }

    public Class<?> getPluginClass() {
        return metadata.getPluginClass();
    }

    public Resource getConfigResource() {
        return configResource;
    }

    public PluginMetadata getMetadata() {
        return metadata;
    }

    public String[] getLoadAfterNames() {
        return metadata.getLoadAfterNames();
    }

    public String[] getLoadBeforeNames() {
        return metadata.getLoadBeforeNames();
    }

    public String[] getDependsOnNames() {
        return metadata.getDependsOnNames();
    }

    public String[] getObservedPluginNames() {
        return metadata.getObservedPluginNames();
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public String getGrailsVersion() {
        return getMetadata().getGrailsVersion();
    }

    public String[] getEvictions() {
        return metadata.getEvictions();
    }

    public PluginDescriptor getPluginDescriptor() {
        return pluginDescriptor;
    }

    public boolean isGrailsVersionCompatible(String grailsVersion) {
        return PluginUtils.isPluginVersionCompatible(getPluginVersion(), getGrailsVersion(), grailsVersion, getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PluginInfo other)) return false;
        return metadata.equals(other.metadata);
    }

    @Override
    public int hashCode() {
        return metadata.hashCode();
    }

    @Override
    public String toString() {
        return "GrailsPluginInfo[" + metadata.getName() + "]";
    }
}
