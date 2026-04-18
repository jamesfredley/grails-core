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
package org.grails.plugins.sitemesh3;

import java.util.Locale;

import jakarta.servlet.ServletContext;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.ContentProcessor;

import org.springframework.core.Ordered;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * {@link ViewResolver} that wraps an inner view resolver and decorates every
 * resolved view with a {@link Sitemesh3LayoutView}. Layout-path view names
 * (e.g. {@code /layouts/foo}) are passed through un-wrapped so decorator
 * renders themselves do not trigger nested decoration.
 */
public class Sitemesh3LayoutViewResolver implements ViewResolver, Ordered {

    private static final String LAYOUT_VIEW_PREFIX = "/layouts";

    private final ViewResolver innerViewResolver;
    private final ContentProcessor contentProcessor;
    private final DecoratorSelector<SiteMeshContext> decoratorSelector;
    private final ServletContext servletContext;

    private int order = Ordered.LOWEST_PRECEDENCE - 30;

    public Sitemesh3LayoutViewResolver(ViewResolver innerViewResolver,
                                       ContentProcessor contentProcessor,
                                       DecoratorSelector<SiteMeshContext> decoratorSelector,
                                       ServletContext servletContext) {
        this.innerViewResolver = innerViewResolver;
        this.contentProcessor = contentProcessor;
        this.decoratorSelector = decoratorSelector;
        this.servletContext = servletContext;
    }

    @Override
    public View resolveViewName(String viewName, Locale locale) throws Exception {
        View innerView = innerViewResolver.resolveViewName(viewName, locale);
        if (innerView == null) {
            return null;
        }
        if (innerView instanceof SmartView && ((SmartView) innerView).isRedirectView()) {
            return innerView;
        }
        if (viewName != null && viewName.startsWith(LAYOUT_VIEW_PREFIX)) {
            return innerView;
        }
        if (innerView instanceof Sitemesh3LayoutView) {
            return innerView;
        }
        return new Sitemesh3LayoutView(innerView, contentProcessor, decoratorSelector, servletContext, innerViewResolver);
    }

    public ViewResolver getInnerViewResolver() {
        return innerViewResolver;
    }

    @Override
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
