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

import jakarta.servlet.ServletContext

import org.sitemesh.DecoratorSelector
import org.sitemesh.SiteMeshContext
import org.sitemesh.content.ContentProcessor

import org.springframework.web.servlet.View
import org.springframework.web.servlet.ViewResolver
import org.springframework.web.servlet.SmartView
import spock.lang.Specification
import spock.lang.Unroll

class Sitemesh3LayoutViewResolverSpec extends Specification {

    ViewResolver inner
    ContentProcessor contentProcessor
    DecoratorSelector<SiteMeshContext> decoratorSelector
    ServletContext servletContext
    Sitemesh3LayoutViewResolver resolver

    def setup() {
        inner = Mock(ViewResolver)
        contentProcessor = Mock(ContentProcessor)
        decoratorSelector = Mock(DecoratorSelector)
        servletContext = Mock(ServletContext)
        resolver = new Sitemesh3LayoutViewResolver(inner, contentProcessor, decoratorSelector, servletContext)
    }

    void 'returns null when inner resolver yields null'() {
        given:
        inner.resolveViewName('missing', Locale.ENGLISH) >> null

        when:
        View resolved = resolver.resolveViewName('missing', Locale.ENGLISH)

        then:
        resolved == null
    }

    void 'redirect views pass through unwrapped'() {
        given:
        View redirect = Mock(View.class.isAssignableFrom(SmartView) ? View : SmartView)
        View redirectSmart = Mock(RedirectSmartView)
        redirectSmart.isRedirectView() >> true
        inner.resolveViewName('/redirect', Locale.ENGLISH) >> redirectSmart

        when:
        View resolved = resolver.resolveViewName('/redirect', Locale.ENGLISH)

        then:
        resolved.is(redirectSmart)
    }

    void 'wrapping is skipped for views already wrapped'() {
        given:
        Sitemesh3LayoutView already = Mock(Sitemesh3LayoutView)
        inner.resolveViewName('page', Locale.ENGLISH) >> already

        when:
        View resolved = resolver.resolveViewName('page', Locale.ENGLISH)

        then:
        resolved.is(already)
    }

    @Unroll
    void 'layout-folder view #viewName is not wrapped'() {
        given:
        View innerView = Mock(View)
        inner.resolveViewName(viewName, Locale.ENGLISH) >> innerView

        when:
        View resolved = resolver.resolveViewName(viewName, Locale.ENGLISH)

        then:
        resolved.is(innerView)

        where:
        viewName << ['/layouts', '/layouts/application', '/layouts/admin/dashboard']
    }

    @Unroll
    void 'view #viewName that shares the layouts prefix but is not under /layouts/ gets wrapped'() {
        given:
        View innerView = Mock(View)
        inner.resolveViewName(viewName, Locale.ENGLISH) >> innerView

        when:
        View resolved = resolver.resolveViewName(viewName, Locale.ENGLISH)

        then:
        resolved instanceof Sitemesh3LayoutView
        !resolved.is(innerView)

        where:
        viewName << ['/layoutsManagement/index', '/layoutsFoo', '/layouts-admin/show']
    }

    void 'a regular view gets wrapped in Sitemesh3LayoutView'() {
        given:
        View innerView = Mock(View)
        inner.resolveViewName('book/show', Locale.ENGLISH) >> innerView

        when:
        View resolved = resolver.resolveViewName('book/show', Locale.ENGLISH)

        then:
        resolved instanceof Sitemesh3LayoutView
        ((Sitemesh3LayoutView) resolved).innerView.is(innerView)
    }

    static interface RedirectSmartView extends View, SmartView {}
}
