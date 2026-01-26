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
package org.grails.web.config.http;

/**
 * Stores the default order numbers of all Grails filters for use in configuration.
 * These filters are run prior to the Spring Security Filter Chain which is at DEFAULT_FILTER_ORDER
 * @since 7.0
 */
public enum GrailsFilters {

    FIRST,
    ASSET_PIPELINE_FILTER,
    CHARACTER_ENCODING_FILTER,
    HIDDEN_HTTP_METHOD_FILTER,
    SITEMESH_FILTER,
    GRAILS_WEB_REQUEST_FILTER,
    LAST(-110);  // DEFAULT_FILTER_ORDER (-100) minus 10

    /**
     * The default order of the Spring Security filter chain.
     * This value was previously available as {@code SecurityProperties.DEFAULT_FILTER_ORDER}
     * but was removed in Spring Boot 4.0. The value -100 ensures Grails filters run
     * before the security filter chain.
     */
    public static final int DEFAULT_FILTER_ORDER = -100;

    private static final int INTERVAL = 10;
    private final int order;

    GrailsFilters() {
        this.order = DEFAULT_FILTER_ORDER - 100 + ordinal() * INTERVAL;
    }

    GrailsFilters(int order) {
        this.order = order;
    }

    public int getOrder() {
        return this.order;
    }

}
