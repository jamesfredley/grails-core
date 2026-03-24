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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.Resource;

public class ClasspathPluginFinder {

    private static final Logger LOG = LoggerFactory.getLogger(ClasspathPluginFinder.class);

    /**
     * Discovers all plugin classes by scanning {@code META-INF/grails-plugin.xml}
     * descriptors on the classpath via {@link PluginUtils#scanPluginDescriptors},
     * then reads ordering metadata and configuration resource location from each plugin
     * class via {@link PluginUtils#extractPluginMetadata} and
     * {@link PluginUtils#readPluginConfiguration}.
     */
    public List<PluginInfo> findClasspathPlugins(ClassLoader classLoader, String targetGrailsVersion) {
        List<PluginDescriptor> pluginDescriptors = PluginUtils.scanPluginDescriptorResources(classLoader);
        if (pluginDescriptors.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<PluginInfo> discoveredPlugins = new ArrayList<>();

        LOG.debug("Attempting to load [{}] plugin descriptors", pluginDescriptors.size());
        for (PluginDescriptor pluginDescriptor : pluginDescriptors) {
            for (String pluginClassName : pluginDescriptor.getProvidedPlugins()) {
                try {
                    Class<?> pluginClass = attemptPluginClassLoad(pluginClassName, classLoader);
                    if (PluginUtils.isGrailsPluginLoadable(pluginClass)) {
                        PluginMetadata metadata = PluginUtils.extractPluginMetadata(pluginClass);
                        if (metadata != null) {
                            Resource configResource = PluginUtils.readPluginConfiguration(pluginClass);
                            PluginInfo pluginInfo = new PluginInfo(pluginDescriptor, metadata, configResource, false);
                            pluginInfo.isGrailsVersionCompatible(targetGrailsVersion);
                            discoveredPlugins.add(pluginInfo);
                        }
                    }
                } catch (Exception e) {
                    LOG.debug("Error loading plugin class [{}]: {}", pluginClassName, e.getMessage());
                }
            }
        }

        return discoveredPlugins;
    }

    private static Class<?> attemptPluginClassLoad(String pluginClassName, ClassLoader classLoader) {
        try {
            return classLoader.loadClass(pluginClassName);
        } catch (ClassNotFoundException e) {
            LOG.warn("Grails Plugin [{}] not found, resuming load without..", pluginClassName);
            if (LOG.isDebugEnabled()) {
                LOG.debug(e.getMessage(), e);
            }
        }
        return null;
    }
}
