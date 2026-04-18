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

import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

import org.sitemesh.content.ContentProcessor
import org.sitemesh.webapp.contentfilter.ResponseMetaData

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import org.springframework.web.servlet.View
import org.springframework.web.servlet.ViewResolver

import spock.lang.Specification

class GrailsViewDispatchContextSpec extends Specification {

    MockHttpServletRequest request
    MockHttpServletResponse response
    MockServletContext servletContext
    ContentProcessor contentProcessor
    ViewResolver viewResolver
    GrailsViewDispatchContext context

    def setup() {
        request = new MockHttpServletRequest()
        response = new MockHttpServletResponse()
        servletContext = new MockServletContext()
        contentProcessor = Mock(ContentProcessor)
        viewResolver = Mock(ViewResolver)
        context = new GrailsViewDispatchContext(
                'text/html', request, response, servletContext,
                contentProcessor, new ResponseMetaData(), false,
                viewResolver, Locale.ENGLISH)
    }

    void 'dispatch resolves the view via the view resolver and renders it'() {
        given:
        View view = Mock(View)

        when:
        context.dispatch(request, response, '/layouts/application')

        then:
        1 * viewResolver.resolveViewName('/layouts/application', Locale.ENGLISH) >> view
        1 * view.render(_ as Map, request, response)
        0 * _
    }

    void 'dispatch raises ServletException when the view is missing'() {
        when:
        context.dispatch(request, response, '/layouts/missing')

        then:
        1 * viewResolver.resolveViewName('/layouts/missing', Locale.ENGLISH) >> null
        thrown(ServletException)
    }

    void 'dispatch wraps view resolution failures in ServletException'() {
        when:
        context.dispatch(request, response, '/layouts/broken')

        then:
        1 * viewResolver.resolveViewName('/layouts/broken', Locale.ENGLISH) >> {
            throw new RuntimeException('boom')
        }
        thrown(ServletException)
    }
}
