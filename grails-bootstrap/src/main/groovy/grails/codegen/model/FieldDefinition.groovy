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
package grails.codegen.model

import groovy.transform.CompileStatic

/**
 * Represents a field definition for domain class code generation.
 *
 * @since 7.0
 */
@CompileStatic
class FieldDefinition {

    /**
     * Constraint style options for field validation.
     */
    enum ConstraintStyle {
        /** Use Grails static constraints block */
        GRAILS,
        /** Use Jakarta Validation annotations */
        JAKARTA,
        /** Use both Grails constraints and Jakarta annotations */
        BOTH
    }

    static final Set<String> SUPPORTED_TYPES = [
        'String', 'Integer', 'Long', 'Boolean', 'Date', 'BigDecimal',
        'Double', 'Float', 'Short', 'Byte', 'Character'
    ] as Set

    String name
    String type = 'String'
    Boolean nullable
    Boolean blank
    Integer maxSize
    Integer minSize
    ConstraintStyle constraintStyle = ConstraintStyle.GRAILS

    /**
     * Parses a field specification string like "title:String" into a FieldDefinition.
     *
     * @param fieldSpec the field specification (e.g., "title:String" or just "title")
     * @return a new FieldDefinition instance
     */
    static FieldDefinition parse(String fieldSpec) {
        if (!fieldSpec) {
            throw new IllegalArgumentException('Field specification cannot be null or empty')
        }

        def parts = fieldSpec.split(':', 2)
        def definition = new FieldDefinition()
        definition.name = parts[0].trim()

        if (parts.length > 1 && parts[1]?.trim()) {
            definition.type = parts[1].trim()
        }

        definition
    }

    /**
     * Validates the field definition.
     *
     * @throws IllegalArgumentException if validation fails
     */
    void validate() {
        if (!name || !name.trim()) {
            throw new IllegalArgumentException('Field name is required')
        }

        if (!name.matches(/^[a-z][a-zA-Z0-9]*$/)) {
            throw new IllegalArgumentException("Field name '${name}' must start with a lowercase letter and contain only alphanumeric characters")
        }

        if (!SUPPORTED_TYPES.contains(type)) {
            throw new IllegalArgumentException("Unsupported field type '${type}'. Supported types: ${SUPPORTED_TYPES.join(', ')}")
        }

        if (blank != null && type != 'String') {
            throw new IllegalArgumentException("The 'blank' constraint is only applicable to String fields")
        }

        if (maxSize != null && type != 'String') {
            throw new IllegalArgumentException("The 'maxSize' constraint is only applicable to String fields")
        }

        if (minSize != null && type != 'String') {
            throw new IllegalArgumentException("The 'minSize' constraint is only applicable to String fields")
        }
    }

    /**
     * Generates the field declaration line for the domain class.
     *
     * @return the field declaration (e.g., "String title")
     */
    String toFieldDeclaration() {
        "${type} ${name}"
    }

    /**
     * Generates the constraint line for the constraints block.
     * Returns null if no constraints are needed.
     *
     * @return the constraint line (e.g., "title blank: false, maxSize: 255") or null
     */
    String toConstraintLine() {
        List<String> constraints = []

        if (nullable != null) {
            constraints.add('nullable: ' + nullable)
        }

        if (blank != null) {
            constraints.add('blank: ' + blank)
        }

        if (maxSize != null) {
            constraints.add('maxSize: ' + maxSize)
        }

        if (minSize != null) {
            constraints.add('minSize: ' + minSize)
        }

        if (constraints.empty) {
            return null
        }

        "${name} ${constraints.join(', ')}"
    }

    /**
     * Generates Jakarta Validation annotations for the field.
     * Returns an empty list if no annotations are needed.
     *
     * @return list of annotation strings (e.g., ["@NotNull", "@Size(max = 255)"])
     */
    List<String> toAnnotations() {
        List<String> annotations = []

        if (nullable != null && !nullable) {
            annotations.add('@NotNull')
        }

        if (blank != null && !blank && type == 'String') {
            annotations.add('@NotBlank')
        }

        if (maxSize != null || minSize != null) {
            List<String> sizeParams = []
            if (minSize != null) {
                sizeParams.add('min = ' + minSize)
            }
            if (maxSize != null) {
                sizeParams.add('max = ' + maxSize)
            }
            annotations.add('@Size(' + sizeParams.join(', ') + ')')
        }

        annotations
    }

    /**
     * Gets the required import statements for Jakarta Validation annotations.
     *
     * @return set of fully qualified import class names
     */
    Set<String> getRequiredImports() {
        Set<String> imports = [] as Set

        if (nullable != null && !nullable) {
            imports.add('jakarta.validation.constraints.NotNull')
        }

        if (blank != null && !blank && type == 'String') {
            imports.add('jakarta.validation.constraints.NotBlank')
        }

        if (maxSize != null || minSize != null) {
            imports.add('jakarta.validation.constraints.Size')
        }

        imports
    }

    /**
     * Determines if this field should use Grails constraints.
     */
    boolean usesGrailsConstraints() {
        constraintStyle == ConstraintStyle.GRAILS || constraintStyle == ConstraintStyle.BOTH
    }

    /**
     * Determines if this field should use Jakarta annotations.
     */
    boolean usesJakartaAnnotations() {
        constraintStyle == ConstraintStyle.JAKARTA || constraintStyle == ConstraintStyle.BOTH
    }

    /**
     * Builder pattern for creating FieldDefinition instances.
     */
    static class Builder {
        private FieldDefinition definition = new FieldDefinition()

        Builder name(String name) {
            definition.name = name
            this
        }

        Builder type(String type) {
            definition.type = type
            this
        }

        Builder nullable(Boolean nullable) {
            definition.nullable = nullable
            this
        }

        Builder blank(Boolean blank) {
            definition.blank = blank
            this
        }

        Builder maxSize(Integer maxSize) {
            definition.maxSize = maxSize
            this
        }

        Builder minSize(Integer minSize) {
            definition.minSize = minSize
            this
        }

        Builder constraintStyle(ConstraintStyle constraintStyle) {
            definition.constraintStyle = constraintStyle
            this
        }

        FieldDefinition build() {
            definition.validate()
            definition
        }
    }

    static Builder builder() {
        new Builder()
    }
}
