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

package com.demo

import org.springframework.beans.factory.annotation.Autowired

/**
 * Controller exposing advanced caching scenarios via HTTP endpoints.
 */
class AdvancedCachingController {

    @Autowired
    AdvancedCachingService advancedCachingService

    // ========== null value endpoints ==========

    def dataOrNull(String input) {
        try {
            def data = advancedCachingService.getDataOrNull(input)
            render(contentType: 'application/json') {
                [data: data]
            }
        } catch (Exception e) {
            render(status: 500, contentType: 'application/json') {
                [error: e.message]
            }
        }
    }

    // ========== exception handling endpoints ==========

    def dataOrThrow(String input) {
        try {
            def data = advancedCachingService.getDataOrThrow(input)
            render(contentType: 'application/json') {
                [data: data]
            }
        } catch (Exception e) {
            render(status: 500, contentType: 'application/json') {
                [error: e.message]
            }
        }
    }

    // ========== collection endpoints ==========

    def listData(String category) {
        try {
            def data = advancedCachingService.getListData(category)
            render(contentType: 'application/json') {
                [data: data]
            }
        } catch (Exception e) {
            render(status: 500, contentType: 'application/json') {
                [error: e.message]
            }
        }
    }

    def mapData(String key) {
        try {
            def data = advancedCachingService.getMapData(key)
            render(contentType: 'application/json') {
                [data: data]
            }
        } catch (Exception e) {
            render(status: 500, contentType: 'application/json') {
                [error: e.message]
            }
        }
    }

    // ========== custom key caching endpoints ==========

    def getDataByKey(String key) {
        try {
            def data = advancedCachingService.getDataByKey(key)
            render(contentType: 'application/json') {
                [data: data]
            }
        } catch (Exception e) {
            render(status: 500, contentType: 'application/json') {
                [error: e.message]
            }
        }
    }

    // ========== eviction endpoints ==========

    def evictNullCache() {
        advancedCachingService.evictNullCache()
        render(contentType: 'application/json') {
            [status: 'evicted']
        }
    }

    def evictExceptionCache() {
        advancedCachingService.evictExceptionCache()
        render(contentType: 'application/json') {
            [status: 'evicted']
        }
    }

    def evictListCache() {
        advancedCachingService.evictListCache()
        render(contentType: 'application/json') {
            [status: 'evicted']
        }
    }

    def evictMapCache() {
        advancedCachingService.evictMapCache()
        render(contentType: 'application/json') {
            [status: 'evicted']
        }
    }

    def evictByKey(String key) {
        advancedCachingService.evictByKey(key)
        render(contentType: 'application/json') {
            [status: 'evicted']
        }
    }

    def evictAllKeyCache() {
        advancedCachingService.evictAllKeyCache()
        render(contentType: 'application/json') {
            [status: 'evicted']
        }
    }
}
