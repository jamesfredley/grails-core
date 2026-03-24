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

import java.util.List;
import java.util.Objects;

import org.springframework.core.io.Resource;

/**
 * Holds a reference to the parsed grails-plugin.xml descriptor or the resource file that represents the plugin for the
 * given plugin classes
 */
public class PluginDescriptor {

    private final Resource resource;
    private final List<String> providedPlugins;
    private final List<String> providedClasses;

    public PluginDescriptor(
            Resource resource,
            List<String> providedPlugins,
            List<String> providedClasses) {
        this.resource = resource;
        this.providedPlugins = providedPlugins;
        this.providedClasses = providedClasses;
    }

    public Resource getResource() {
        return resource;
    }

    public List<String> getProvidedPlugins() {
        return providedPlugins;
    }

    public List<String> getProvidedClasses() {
        return providedClasses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PluginDescriptor that)) return false;
        return Objects.equals(resource, that.resource) &&
                Objects.equals(providedPlugins, that.providedPlugins) &&
                Objects.equals(providedClasses, that.providedClasses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, providedPlugins, providedClasses);
    }

    @Override
    public String toString() {
        return "PluginDescriptor[" +
                "resource=" + resource +
                ", providedPlugins=" + providedPlugins +
                ", providedClasses=" + providedClasses +
                ']';
    }
}
