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
package org.grails.datastore.mapping.model;

import java.lang.reflect.Field;

import org.springframework.util.ReflectionUtils;

import grails.util.Environment;
import org.grails.datastore.mapping.config.Property;
import org.grails.datastore.mapping.config.Property.AutoTimestampType;

/**
 * Utility class for detecting and caching auto-timestamp and auditing annotations on domain properties.
 * This avoids repeated reflection calls by storing the annotation type in the Property metadata.
 *
 * <p>Supports the following annotations (both GORM and Spring Data variants):</p>
 * <ul>
 *   <li>@CreatedDate / @grails.gorm.annotation.CreatedDate - automatically set on insert</li>
 *   <li>@LastModifiedDate / @grails.gorm.annotation.LastModifiedDate - automatically set on insert and update</li>
 *   <li>@CreatedBy / @grails.gorm.annotation.CreatedBy - automatically populated with current auditor on insert</li>
 *   <li>@LastModifiedBy / @grails.gorm.annotation.LastModifiedBy - automatically populated with current auditor on insert and update</li>
 *   <li>@AutoTimestamp - GORM-specific annotation for backwards compatibility</li>
 * </ul>
 *
 * <p>Caching is automatically disabled in development mode ({@link Environment#isDevelopmentMode()})
 * to ensure annotation changes are picked up during class reloading.</p>
 *
 * @author Scott Murphy Heiberg
 * @since 7.0
 */
public class AutoTimestampUtils {

    private static final String CREATED_DATE_ANNOTATION = "grails.gorm.annotation.CreatedDate";
    private static final String LAST_MODIFIED_DATE_ANNOTATION = "grails.gorm.annotation.LastModifiedDate";
    private static final String AUTO_TIMESTAMP_ANNOTATION = "grails.gorm.annotation.AutoTimestamp";
    private static final String CREATED_BY_ANNOTATION = "grails.gorm.annotation.CreatedBy";
    private static final String LAST_MODIFIED_BY_ANNOTATION = "grails.gorm.annotation.LastModifiedBy";

    private static final String CREATED_DATE_SPRING_ANNOTATION = "org.springframework.data.annotation.CreatedDate";
    private static final String LAST_MODIFIED_DATE_SPRING_ANNOTATION = "org.springframework.data.annotation.LastModifiedDate";
    private static final String CREATED_BY_SPRING_ANNOTATION = "org.springframework.data.annotation.CreatedBy";
    private static final String LAST_MODIFIED_BY_SPRING_ANNOTATION = "org.springframework.data.annotation.LastModifiedBy";

    /**
     * Gets the auto-timestamp type for a persistent property, using cached metadata when not in development mode.
     *
     * <p>In development mode, this method will always perform reflection to detect the current
     * annotation state, ensuring that annotation changes during class reloading are immediately
     * recognized. In production, the result is cached to avoid repeated reflection calls.</p>
     *
     * @param persistentProperty The persistent property to check
     * @return The auto-timestamp type (CREATED, UPDATED, or NONE)
     */
    public static AutoTimestampType getAutoTimestampType(PersistentProperty<?> persistentProperty) {
        Property mappedForm = persistentProperty.getMapping().getMappedForm();

        // In development mode, always detect fresh to support class reloading
        if (Environment.isDevelopmentMode()) {
            return detectAutoTimestampType(persistentProperty);
        }

        // Return cached value if available
        if (mappedForm.getAutoTimestampType() != null) {
            return mappedForm.getAutoTimestampType();
        }

        // Detect and cache the annotation type
        AutoTimestampType type = detectAutoTimestampType(persistentProperty);
        mappedForm.setAutoTimestampType(type);
        return type;
    }

    /**
     * Detects the auto-timestamp annotation type on a property using reflection.
     *
     * <p>When caching is enabled (production mode), this method is called once per property
     * and the result is cached. When caching is disabled (development mode), this method
     * is called on every access to ensure annotation changes are detected.</p>
     *
     * @param persistentProperty The persistent property to check
     * @return The auto-timestamp type (CREATED, UPDATED, or NONE)
     */
    private static AutoTimestampType detectAutoTimestampType(PersistentProperty<?> persistentProperty) {
        try {
            Field field = ReflectionUtils.findField(
                persistentProperty.getOwner().getJavaClass(),
                persistentProperty.getName()
            );

            if (field != null) {
                for (java.lang.annotation.Annotation annotation : field.getDeclaredAnnotations()) {
                    String annotationName = annotation.annotationType().getName();

                    if (CREATED_DATE_ANNOTATION.equals(annotationName) ||
                        CREATED_DATE_SPRING_ANNOTATION.equals(annotationName)) {
                        return AutoTimestampType.CREATED;
                    } else if (LAST_MODIFIED_DATE_ANNOTATION.equals(annotationName) ||
                               LAST_MODIFIED_DATE_SPRING_ANNOTATION.equals(annotationName)) {
                        return AutoTimestampType.UPDATED;
                    } else if (CREATED_BY_ANNOTATION.equals(annotationName) ||
                               CREATED_BY_SPRING_ANNOTATION.equals(annotationName)) {
                        return AutoTimestampType.CREATED_BY;
                    } else if (LAST_MODIFIED_BY_ANNOTATION.equals(annotationName) ||
                               LAST_MODIFIED_BY_SPRING_ANNOTATION.equals(annotationName)) {
                        return AutoTimestampType.UPDATED_BY;
                    } else if (AUTO_TIMESTAMP_ANNOTATION.equals(annotationName)) {
                        // For @AutoTimestamp, check the EventType value
                        try {
                            Object eventTypeValue = annotation.annotationType()
                                .getMethod("value")
                                .invoke(annotation);

                            if (eventTypeValue != null) {
                                String eventTypeName = eventTypeValue.toString();
                                if (eventTypeName.equals("UPDATED")) {
                                    return AutoTimestampType.UPDATED;
                                } else {
                                    return AutoTimestampType.CREATED;
                                }
                            }
                        } catch (Exception e) {
                            // If we can't read the value, default to CREATED
                            return AutoTimestampType.CREATED;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // If reflection fails, return NONE
        }

        return AutoTimestampType.NONE;
    }

    /**
     * Checks if a property has any auto-timestamp or auditing annotation.
     *
     * @param persistentProperty The persistent property to check
     * @return true if the property has any supported annotation (@CreatedDate, @LastModifiedDate,
     *         @CreatedBy, @LastModifiedBy, or @AutoTimestamp) from either GORM or Spring Data
     */
    public static boolean hasAutoTimestampAnnotation(PersistentProperty<?> persistentProperty) {
        return persistentProperty != null && getAutoTimestampType(persistentProperty) != AutoTimestampType.NONE;
    }

    /**
     * Checks if a property has a @CreatedDate annotation (GORM or Spring Data) or @AutoTimestamp(CREATED).
     *
     * @param persistentProperty The persistent property to check
     * @return true if the property represents a creation timestamp
     */
    public static boolean isCreatedTimestamp(PersistentProperty<?> persistentProperty) {
        return getAutoTimestampType(persistentProperty) == AutoTimestampType.CREATED;
    }

    /**
     * Checks if a property has a @LastModifiedDate annotation (GORM or Spring Data) or @AutoTimestamp(UPDATED).
     *
     * @param persistentProperty The persistent property to check
     * @return true if the property represents an update timestamp
     */
    public static boolean isUpdatedTimestamp(PersistentProperty<?> persistentProperty) {
        return getAutoTimestampType(persistentProperty) == AutoTimestampType.UPDATED;
    }

    /**
     * Checks if a property has a @CreatedBy annotation (GORM or Spring Data).
     *
     * @param persistentProperty The persistent property to check
     * @return true if the property represents a creation auditor
     */
    public static boolean isCreatedBy(PersistentProperty<?> persistentProperty) {
        return getAutoTimestampType(persistentProperty) == AutoTimestampType.CREATED_BY;
    }

    /**
     * Checks if a property has a @LastModifiedBy annotation (GORM or Spring Data).
     *
     * @param persistentProperty The persistent property to check
     * @return true if the property represents an update auditor
     */
    public static boolean isUpdatedBy(PersistentProperty<?> persistentProperty) {
        return getAutoTimestampType(persistentProperty) == AutoTimestampType.UPDATED_BY;
    }
}
