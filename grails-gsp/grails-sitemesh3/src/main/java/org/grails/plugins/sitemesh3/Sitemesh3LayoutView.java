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

import java.io.PrintWriter;
import java.nio.CharBuffer;
import java.util.Map;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.webapp.contentfilter.ResponseMetaData;

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * Spring MVC {@link View} that wraps an inner view, renders it into a buffered
 * response, and applies SiteMesh 3 decoration before flushing to the real
 * response. Forwarded/error dispatches are handled the same way as in
 * grails-layout's EmbeddedGrailsLayoutView; includes are passed straight
 * through so sub-renders are not decorated.
 */
public class Sitemesh3LayoutView implements View {

    private final View innerView;
    private final ContentProcessor contentProcessor;
    private final DecoratorSelector<SiteMeshContext> decoratorSelector;
    private final ServletContext servletContext;
    private final ViewResolver viewResolver;

    public Sitemesh3LayoutView(View innerView,
                               ContentProcessor contentProcessor,
                               DecoratorSelector<SiteMeshContext> decoratorSelector,
                               ServletContext servletContext,
                               ViewResolver viewResolver) {
        this.innerView = innerView;
        this.contentProcessor = contentProcessor;
        this.decoratorSelector = decoratorSelector;
        this.servletContext = servletContext;
        this.viewResolver = viewResolver;
    }

    @Override
    public String getContentType() {
        return innerView.getContentType();
    }

    public View getInnerView() {
        return innerView;
    }

    @Override
    public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (request.getDispatcherType() == DispatcherType.INCLUDE) {
            innerView.render(model, request, response);
            return;
        }

        Object previousCaptured = request.getAttribute(Sitemesh3CapturedPage.REQUEST_ATTRIBUTE);
        Sitemesh3CapturedPage capturedPage = new Sitemesh3CapturedPage();
        request.setAttribute(Sitemesh3CapturedPage.REQUEST_ATTRIBUTE, capturedPage);

        try {
            ResponseMetaData metaData = new ResponseMetaData();
            GrailsViewDispatchContext context = new GrailsViewDispatchContext(
                    response.getContentType() != null ? response.getContentType() : "text/html",
                    request, response, servletContext,
                    contentProcessor, metaData, false,
                    viewResolver, request.getLocale());

            GrailsContentBufferingResponse bufferedResponse = new GrailsContentBufferingResponse(
                    request, response, contentProcessor, context, metaData);

            innerView.render(model, request, bufferedResponse);

            Content content = bufferedResponse.getContent();

            if (content == null) {
                CharBuffer raw = bufferedResponse.getBufferedBody();
                if (raw != null) {
                    response.getWriter().append(raw);
                }
                return;
            }

            applyMetaHttpEquivContentType(content, response);

            DispatcherType type = request.getDispatcherType();
            if (type == DispatcherType.ASYNC || type == DispatcherType.ERROR
                    || type == DispatcherType.FORWARD || type == DispatcherType.REQUEST) {
                String[] decoratorPaths = decoratorSelector.selectDecoratorPaths(content, context);
                if (decoratorPaths != null && decoratorPaths.length > 0) {
                    Content decorated = content;
                    for (String path : decoratorPaths) {
                        if (path == null) {
                            continue;
                        }
                        decorated = context.decorate(path, decorated);
                        if (decorated == null) {
                            break;
                        }
                    }
                    if (decorated != null) {
                        PrintWriter writer = response.getWriter();
                        decorated.getData().writeValueTo(writer);
                        if (!response.isCommitted()) {
                            writer.flush();
                        }
                        return;
                    }
                }
            }

            PrintWriter writer = response.getWriter();
            content.getData().writeValueTo(writer);
            if (!response.isCommitted()) {
                writer.flush();
            }
        } finally {
            if (previousCaptured != null) {
                request.setAttribute(Sitemesh3CapturedPage.REQUEST_ATTRIBUTE, previousCaptured);
            } else {
                request.removeAttribute(Sitemesh3CapturedPage.REQUEST_ATTRIBUTE);
            }
        }
    }

    private void applyMetaHttpEquivContentType(Content content, HttpServletResponse response) {
        String contentType = content.getExtractedProperties()
                .getChild("meta").getChild("http-equiv").getChild("Content-Type").getValue();
        if (contentType != null && "text/html".equals(response.getContentType())) {
            response.setContentType(contentType);
        }
    }
}
