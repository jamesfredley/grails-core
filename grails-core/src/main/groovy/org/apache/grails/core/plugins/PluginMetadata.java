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

import java.util.Map;
import java.util.Set;

import grails.plugins.exceptions.PluginException;
import grails.util.Environment;

/**
 * Lightweight value class holding plugin metadata extracted from a plugin
 * class.
 */
public final class PluginMetadata {

    private final String name;
    private final String pluginVersion;
    private final String grailsVersion;
    private final Class<?> pluginClass;
    private final String[] loadAfterNames;
    private final String[] loadBeforeNames;
    private final Map<String, Object> dependencies;
    private final String[] dependsOnNames;
    private final String[] evictions;
    private final String[] observedPluginNames;
    private final Map<String, Set<Object>> environments;
    private final boolean enabled;

    /**
     * @param name the logical plugin name (e.g., "core", "myPlugin")
     * @param grailsVersion the grailsVersion this plugin supports
     * @param pluginClass the plugin's class
     * @param loadAfterNames plugin names this plugin should load after
     * @param loadBeforeNames plugin names this plugin should load before
     * @param dependsOnNames plugin names this plugin depends on (used for
     *        transitive dependency resolution during filtering, not for
     *        sort ordering)
     * @param environments the environments this plugin is enabled for, or empty if enabled for all environments
     * @param enabled if the plugin is enabled
     */
    public PluginMetadata(
            String name,
            String pluginVersion,
            String grailsVersion,
            Class<?> pluginClass,
            String[] loadAfterNames,
            String[] loadBeforeNames,
            Map<String, Object> dependencies,
            String[] dependsOnNames,
            String[] evictions,
            String[] observedPluginNames,
            Map<String, Set<Object>> environments,
            boolean enabled) {
        this.name = name;
        this.pluginVersion = pluginVersion;
        this.grailsVersion = grailsVersion;
        this.pluginClass = pluginClass;
        this.loadAfterNames = loadAfterNames;
        this.loadBeforeNames = loadBeforeNames;
        this.dependencies = dependencies;
        this.dependsOnNames = dependsOnNames;
        this.evictions = evictions;
        this.observedPluginNames = observedPluginNames;
        this.environments = environments;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public String getGrailsVersion() {
        return grailsVersion;
    }

    public Class<?> getPluginClass() {
        return pluginClass;
    }

    public String[] getLoadAfterNames() {
        return loadAfterNames;
    }

    public String[] getLoadBeforeNames() {
        return loadBeforeNames;
    }

    public Map<String, Object> getDependencies() {
        return dependencies;
    }

    public String[] getDependsOnNames() {
        return dependsOnNames;
    }

    public String[] getEvictions() {
        return evictions;
    }

    public String[] getObservedPluginNames() {
        return observedPluginNames;
    }

    public Map<String, Set<Object>> getEnvironments() {
        return environments;
    }

    public boolean getEnabled() {
        return enabled;
    }

    boolean canRegisterPlugin() {
        Environment environment = Environment.getCurrent();
        return enabled && supportsEnvironment(environment);
    }

    boolean supportsEnvironment(Environment environment) {
        return PluginUtils.supportsValueInIncludeExcludeMap(environments, environment.getName());
    }

    public String getDependentVersion(String name) {
        Object dependentVersion = dependencies.get(name);
        if (dependentVersion == null) {
            throw new PluginException("Plugin [" + getName() + "] referenced dependency [" + name + "] with no version!");
        }
        return dependentVersion.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PluginMetadata other)) return false;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "GrailsPluginClassMetadata[" + name + "]";
    }
}
