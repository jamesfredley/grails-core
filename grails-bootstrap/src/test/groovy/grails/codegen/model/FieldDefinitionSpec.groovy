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

import spock.lang.Specification
import spock.lang.Unroll

class FieldDefinitionSpec extends Specification {

    def "should parse simple field specification"() {
        when:
        def field = FieldDefinition.parse("title")

        then:
        field.name == "title"
        field.type == "String"  // default type
    }

    def "should parse field specification with type"() {
        when:
        def field = FieldDefinition.parse("count:Integer")

        then:
        field.name == "count"
        field.type == "Integer"
    }

    def "should throw exception for null field spec"() {
        when:
        FieldDefinition.parse(null)

        then:
        thrown(IllegalArgumentException)
    }

    def "should throw exception for empty field spec"() {
        when:
        FieldDefinition.parse("")

        then:
        thrown(IllegalArgumentException)
    }

    def "should validate valid field names"() {
        given:
        def field = new FieldDefinition(name: name, type: 'String')

        when:
        field.validate()

        then:
        noExceptionThrown()

        where:
        name << ['title', 'firstName', 'count1']
    }

    @Unroll
    def "should reject invalid field name '#name'"() {
        given:
        def field = new FieldDefinition(name: name, type: 'String')

        when:
        field.validate()

        then:
        thrown(IllegalArgumentException)

        where:
        name << ['Title', '1count', 'first_name', '', null]
    }

    def "should validate supported field types"() {
        given:
        def field = new FieldDefinition(name: 'test', type: type)

        when:
        field.validate()

        then:
        noExceptionThrown()

        where:
        type << ['String', 'Integer', 'Long', 'Boolean', 'Date', 'BigDecimal', 'Double', 'Float']
    }

    @Unroll
    def "should reject unsupported field type '#type'"() {
        given:
        def field = new FieldDefinition(name: 'test', type: type)

        when:
        field.validate()

        then:
        thrown(IllegalArgumentException)

        where:
        type << ['CustomType', 'List']
    }

    def "should reject blank constraint for non-String type"() {
        given:
        def field = new FieldDefinition(name: 'count', type: 'Integer', blank: false)

        when:
        field.validate()

        then:
        thrown(IllegalArgumentException)
    }

    def "should reject maxSize constraint for non-String type"() {
        given:
        def field = new FieldDefinition(name: 'count', type: 'Integer', maxSize: 255)

        when:
        field.validate()

        then:
        thrown(IllegalArgumentException)
    }

    def "should generate field declaration"() {
        given:
        def field = new FieldDefinition(name: 'title', type: 'String')

        expect:
        field.toFieldDeclaration() == 'String title'
    }

    def "should generate constraint line with all constraints"() {
        given:
        def field = new FieldDefinition(
            name: 'title',
            type: 'String',
            nullable: false,
            blank: false,
            maxSize: 255
        )

        expect:
        field.toConstraintLine() == 'title nullable: false, blank: false, maxSize: 255'
    }

    def "should return null constraint line when no constraints"() {
        given:
        def field = new FieldDefinition(name: 'title', type: 'String')

        expect:
        field.toConstraintLine() == null
    }

    def "should generate constraint line with nullable only"() {
        given:
        def field = new FieldDefinition(name: 'description', type: 'String', nullable: true)

        expect:
        field.toConstraintLine() == 'description nullable: true'
    }

    def "should use builder pattern"() {
        when:
        def field = FieldDefinition.builder()
            .name('title')
            .type('String')
            .nullable(false)
            .blank(false)
            .maxSize(255)
            .build()

        then:
        field.name == 'title'
        field.type == 'String'
        field.nullable == false
        field.blank == false
        field.maxSize == 255
    }

    // Jakarta Validation annotation tests

    def "should generate NotNull annotation for nullable false"() {
        given:
        def field = new FieldDefinition(name: 'title', type: 'String', nullable: false)

        expect:
        field.toAnnotations() == ['@NotNull']
    }

    def "should generate NotBlank annotation for blank false on String"() {
        given:
        def field = new FieldDefinition(name: 'title', type: 'String', blank: false)

        expect:
        field.toAnnotations() == ['@NotBlank']
    }

    def "should generate Size annotation for maxSize"() {
        given:
        def field = new FieldDefinition(name: 'title', type: 'String', maxSize: 255)

        expect:
        field.toAnnotations() == ['@Size(max = 255)']
    }

    def "should generate Size annotation with min and max"() {
        given:
        def field = new FieldDefinition(name: 'title', type: 'String', minSize: 5, maxSize: 255)

        expect:
        field.toAnnotations() == ['@Size(min = 5, max = 255)']
    }

    def "should generate multiple annotations"() {
        given:
        def field = new FieldDefinition(
            name: 'title',
            type: 'String',
            nullable: false,
            blank: false,
            maxSize: 255
        )

        expect:
        field.toAnnotations() == ['@NotNull', '@NotBlank', '@Size(max = 255)']
    }

    def "should return empty annotations when no constraints"() {
        given:
        def field = new FieldDefinition(name: 'title', type: 'String')

        expect:
        field.toAnnotations() == []
    }

    def "should return empty annotations when nullable is true"() {
        given:
        def field = new FieldDefinition(name: 'title', type: 'String', nullable: true)

        expect:
        field.toAnnotations() == []
    }

    def "should get required imports for NotNull"() {
        given:
        def field = new FieldDefinition(name: 'title', type: 'String', nullable: false)

        expect:
        field.getRequiredImports() == ['jakarta.validation.constraints.NotNull'] as Set
    }

    def "should get required imports for multiple annotations"() {
        given:
        def field = new FieldDefinition(
            name: 'title',
            type: 'String',
            nullable: false,
            blank: false,
            maxSize: 255
        )

        expect:
        field.getRequiredImports() == [
            'jakarta.validation.constraints.NotNull',
            'jakarta.validation.constraints.NotBlank',
            'jakarta.validation.constraints.Size'
        ] as Set
    }

    def "should default to GRAILS constraint style"() {
        given:
        def field = new FieldDefinition(name: 'title', type: 'String')

        expect:
        field.constraintStyle == FieldDefinition.ConstraintStyle.GRAILS
        field.usesGrailsConstraints()
        !field.usesJakartaAnnotations()
    }

    def "should support JAKARTA constraint style"() {
        given:
        def field = new FieldDefinition(
            name: 'title',
            type: 'String',
            constraintStyle: FieldDefinition.ConstraintStyle.JAKARTA
        )

        expect:
        !field.usesGrailsConstraints()
        field.usesJakartaAnnotations()
    }

    def "should support BOTH constraint style"() {
        given:
        def field = new FieldDefinition(
            name: 'title',
            type: 'String',
            constraintStyle: FieldDefinition.ConstraintStyle.BOTH
        )

        expect:
        field.usesGrailsConstraints()
        field.usesJakartaAnnotations()
    }

    def "should use builder with constraint style"() {
        when:
        def field = FieldDefinition.builder()
            .name('title')
            .type('String')
            .nullable(false)
            .constraintStyle(FieldDefinition.ConstraintStyle.JAKARTA)
            .build()

        then:
        field.constraintStyle == FieldDefinition.ConstraintStyle.JAKARTA
    }
}
