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
package org.grails.plugins.sitemesh3

import groovy.transform.CompileStatic

import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.ConstructorArgumentValues
import org.springframework.beans.factory.config.RuntimeBeanReference
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.beans.factory.support.GenericBeanDefinition
import org.springframework.core.Ordered

/**
 * Renames the primary GSP view resolver (registered under
 * {@code gspViewResolver} and/or {@code jspViewResolver}) and installs a
 * {@link Sitemesh3LayoutViewResolver} under the same name, wrapping the
 * original. The original's bean definition is re-registered under the
 * inner name so it can still be resolved as the delegate.
 *
 * <p>Mirrors the approach used by grails-layout's
 * {@code GrailsLayoutViewResolverPostProcessor}: we need to intercept view
 * resolution at the Spring MVC level rather than via a servlet filter.</p>
 */
@CompileStatic
class Sitemesh3LayoutViewResolverPostProcessor implements BeanDefinitionRegistryPostProcessor, Ordered {

    static final String GSP_VIEW_RESOLVER_BEAN_NAME = 'gspViewResolver'
    static final String JSP_VIEW_RESOLVER_BEAN_NAME = 'jspViewResolver'
    static final String INNER_VIEW_RESOLVER_BEAN_NAME = 'gspViewResolverInner'
    static final String SITEMESH3_LAYOUT_VIEW_RESOLVER_BEAN_NAME = 'sitemesh3LayoutViewResolver'
    static final String CONTENT_PROCESSOR_BEAN_NAME = 'sitemesh3ContentProcessor'
    static final String DECORATOR_SELECTOR_BEAN_NAME = 'sitemesh3DecoratorSelector'

    int order = Ordered.LOWEST_PRECEDENCE - 100

    @Override
    void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        String existingName = findExistingViewResolverName(registry)
        if (existingName == null) {
            return
        }

        BeanDefinition innerDefinition = registry.getBeanDefinition(existingName)
        registry.removeBeanDefinition(existingName)
        registry.registerBeanDefinition(INNER_VIEW_RESOLVER_BEAN_NAME, innerDefinition)

        GenericBeanDefinition wrapperDefinition = new GenericBeanDefinition()
        wrapperDefinition.beanClass = Sitemesh3LayoutViewResolver
        wrapperDefinition.primary = true
        wrapperDefinition.lazyInit = true

        ConstructorArgumentValues constructorArgs = wrapperDefinition.getConstructorArgumentValues()
        constructorArgs.addIndexedArgumentValue(0, new RuntimeBeanReference(INNER_VIEW_RESOLVER_BEAN_NAME))
        constructorArgs.addIndexedArgumentValue(1, new RuntimeBeanReference(CONTENT_PROCESSOR_BEAN_NAME))
        constructorArgs.addIndexedArgumentValue(2, new RuntimeBeanReference(DECORATOR_SELECTOR_BEAN_NAME))
        constructorArgs.addIndexedArgumentValue(3, new RuntimeBeanReference('servletContext'))

        registry.registerBeanDefinition(SITEMESH3_LAYOUT_VIEW_RESOLVER_BEAN_NAME, wrapperDefinition)
        registry.registerAlias(SITEMESH3_LAYOUT_VIEW_RESOLVER_BEAN_NAME, existingName)
    }

    private static String findExistingViewResolverName(BeanDefinitionRegistry registry) {
        if (registry.containsBeanDefinition(GSP_VIEW_RESOLVER_BEAN_NAME)) {
            return GSP_VIEW_RESOLVER_BEAN_NAME
        }
        if (registry.containsBeanDefinition(JSP_VIEW_RESOLVER_BEAN_NAME)) {
            return JSP_VIEW_RESOLVER_BEAN_NAME
        }
        null
    }
}
