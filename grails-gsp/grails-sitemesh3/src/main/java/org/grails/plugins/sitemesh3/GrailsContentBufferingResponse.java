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

import java.io.IOException;
import java.nio.CharBuffer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.sitemesh.SiteMeshContext;
import org.sitemesh.config.PathMapper;
import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.webapp.contentfilter.BasicSelector;
import org.sitemesh.webapp.contentfilter.HttpServletResponseBuffer;
import org.sitemesh.webapp.contentfilter.ResponseMetaData;

/**
 * Buffers the response produced by an inner {@link org.springframework.web.servlet.View}
 * so its body can be handed to SiteMesh 3 for decoration. Short-circuits the
 * content-processor parse whenever the GSP capture taglib has already populated
 * a {@link Sitemesh3CapturedPage} on the current request.
 */
public class GrailsContentBufferingResponse extends HttpServletResponseBuffer {

    private final HttpServletRequest request;
    private final SiteMeshContext context;
    private final ContentProcessor contentProcessor;

    public GrailsContentBufferingResponse(HttpServletRequest request,
                                          HttpServletResponse response,
                                          ContentProcessor contentProcessor,
                                          SiteMeshContext context,
                                          ResponseMetaData metaData) {
        super(response, metaData, new BasicSelector(new PathMapper<Boolean>(), false) {
            @Override
            public boolean shouldBufferForContentType(String contentType, String mimeType, String encoding) {
                return true;
            }
        });
        this.request = request;
        this.context = context;
        this.contentProcessor = contentProcessor;
        String existingContentType = response.getContentType();
        setContentType(existingContentType != null ? existingContentType : "text/html");
    }

    public Content getContent() throws IOException {
        Sitemesh3CapturedPage captured = (Sitemesh3CapturedPage) request.getAttribute(Sitemesh3CapturedPage.REQUEST_ATTRIBUTE);
        if (captured != null && captured.isUsed()) {
            return captured;
        }
        CharBuffer buffer = getBuffer();
        if (buffer == null) {
            return null;
        }
        return contentProcessor.build(buffer, context);
    }

    public CharBuffer getBufferedBody() throws IOException {
        return getBuffer();
    }
}
