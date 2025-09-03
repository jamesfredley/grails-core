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

package grails.plugin.scaffolding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.context.ApplicationContext;

import grails.util.Holders;
import org.grails.datastore.gorm.GormEntity;

/**
 * Resolves the appropriate service bean for a given domain class by:
 *   - Scanning only beans of type GormService
 *   - Matching via: ((GormEntity<?>) service.getResource()).instanceOf(domainClass)
 *
 * Keeps a single shared cache for the whole app.
 */
public final class DomainServiceLocator {

    private static final ConcurrentMap<Class<?>, Object> CACHE = new ConcurrentHashMap<>();

    private DomainServiceLocator() {}

    /** Resolve (and cache) a service bean for the given domain class. */
    public static Object resolve(Class<?> domainClass) {
        return CACHE.computeIfAbsent(domainClass, DomainServiceLocator::findService);
    }

    /** Clear cache (useful in tests/dev reloads). */
    public static void clear() {
        CACHE.clear();
    }

    // ---------------------------------------------------------------------
    // Core resolution
    // ---------------------------------------------------------------------

    private static Object findService(Class<?> domainClass) {
        ApplicationContext ctx = Holders.getGrailsApplication().getMainContext();

        String[] names = ctx.getBeanNamesForType(GormService.class);
        Object match = null;
        List<String> matchingBeanNames = new ArrayList<>();

        for (String name : names) {
            Object bean = ctx.getBean(name);

            // Cast to GormService to call getResource() directly
            @SuppressWarnings("unchecked")
            GormService<?> svc = (GormService<?>) bean;

            Object resource = svc.getResource();
            if (resource instanceof GormEntity) {
                @SuppressWarnings("unchecked")
                GormEntity<?> ge = (GormEntity<?>) resource;
                if (ge.instanceOf(domainClass)) {
                    matchingBeanNames.add(name);
                    if (match == null) {
                        match = bean;
                    } else {
                        // More than one match → ambiguous
                        throw new IllegalStateException(
                            "Multiple GormService beans match domain " + domainClass.getName() +
                            ": " + matchingBeanNames
                        );
                    }
                }
            }
        }

        if (match == null) {
            throw new IllegalStateException(
                "No GormService bean found for domain " + domainClass.getName() +
                " using resource.instanceOf(..). Scanned " + names.length + " GormService beans."
            );
        }

        return match;
    }
}
