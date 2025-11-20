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

package grails.plugin.scaffolding.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Scaffolding annotation for Controllers and Services.
 *
 * <p>At compile-time, this annotation:
 * <ul>
 *   <li>Detects whether applied to a Service or Controller</li>
 *   <li>Sets appropriate parent class (with sensible defaults)</li>
 *   <li>Sets the domain/entity class</li>
 *   <li>Generates required constructor</li>
 * </ul>
 *
 * <h3>Usage Examples</h3>
 *
 * <h4>Services:</h4>
 * <pre>{@code
 * // Simple: domain class only (uses default GormService)
 * @Scaffold(Car.class)
 * class CarService {}  // → extends GormService<Car>
 *
 * // Explicit: specify both base and domain
 * @Scaffold(value = GormService.class, domain = Car.class)
 * class CarService {}  // → extends GormService<Car>
 *
 * // Alternative: domain parameter
 * @Scaffold(domain = Car.class)
 * class CarService {}  // → extends GormService<Car>
 * }</pre>
 *
 * <h4>Controllers:</h4>
 * <pre>{@code
 * // Simple: domain class only (uses default RestfulController)
 * @Scaffold(Car.class)
 * class CarController {}  // → extends RestfulController<Car>
 *
 * // Explicit: specify both base and domain
 * @Scaffold(value = RestfulController.class, domain = Car.class)
 * class CarController {}  // → extends RestfulController<Car>
 * }</pre>
 *
 * @author Scott Murphy Heiberg
 * @since 5.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Scaffold {

    /**
     * Base class to extend OR domain class (context-dependent).
     *
     * <p><b>Interpretation:</b></p>
     * <ul>
     *   <li>If value has generics (e.g., {@code GormService<Car>}):
     *     <ul>
     *       <li>Base class = {@code GormService}</li>
     *       <li>Domain class = {@code Car} (extracted from generic)</li>
     *     </ul>
     *   </li>
     *   <li>If value is a known base class (e.g., {@code GormService}):
     *     <ul>
     *       <li>Base class = value</li>
     *       <li>Domain class = from {@link #domain()} parameter</li>
     *     </ul>
     *   </li>
     *   <li>Otherwise (e.g., {@code Car}):
     *     <ul>
     *       <li>Domain class = value</li>
     *       <li>Base class = default for artefact type</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @return the base class or domain class
     */
    Class<?> value() default Void.class;

    /**
     * Domain/entity class (alternative to value).
     * More explicit when also specifying base class.
     *
     * <p>Examples:</p>
     * <pre>{@code
     * @Scaffold(domain = Car.class)  // Uses default base
     * @Scaffold(value = JpaScaffoldService.class, domain = Car.class)
     * }</pre>
     *
     * @return the domain class
     */
    Class<?> domain() default Void.class;

    /**
     * Whether this service/controller is read-only.
     * Passed to constructor of base class.
     *
     * <p>For services: mutations throw {@code ReadOnlyServiceException}
     * <p>For controllers: mutation endpoints may return 405 Method Not Allowed
     *
     * @return true if read-only, false otherwise
     */
    boolean readOnly() default false;
}
