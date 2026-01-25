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

import grails.codegen.model.AbstractMemberDefinition
import grails.codegen.model.DomainFieldModifier
import grails.codegen.model.FieldDefinition
import org.grails.cli.interactive.completers.DomainClassCompleter

description("Adds a field with access modifier to an existing domain class") {
    usage "grails add-field [DOMAIN CLASS] [FIELD:TYPE] --access=private|protected|public"
    argument name: 'Domain Class', description: "The name of the domain class", required: true
    argument name: 'Field Spec', description: "Field specification in name:Type format (e.g., title:String)", required: true
    completer DomainClassCompleter
    flag name: 'access', description: "Access modifier: private (default), protected, or public"
    flag name: 'nullable', description: "Mark the field as nullable"
    flag name: 'not-nullable', description: "Mark the field as NOT nullable (generates @NotNull)"
    flag name: 'blank', description: "Allow blank values (String fields only)"
    flag name: 'not-blank', description: "Disallow blank values (generates @NotBlank)"
    flag name: 'max-size', description: "Maximum size constraint (String fields only)"
    flag name: 'min-size', description: "Minimum size constraint (String fields only)"
    flag name: 'constraint-style', description: "Constraint style: grails (default), jakarta, or both"
}

if (args.size() < 2) {
    error "Usage: grails add-field [DOMAIN CLASS] [FIELD:TYPE] --access=private|protected|public"
    error "Example: grails add-field Book title:String --access=private --not-nullable"
    return false
}

String domainClassName = args[0]
String fieldSpec = args[1]

FieldDefinition field
try {
    field = FieldDefinition.parse(fieldSpec)
} catch (IllegalArgumentException e) {
    error "Invalid field specification: ${e.message}"
    return false
}

// Determine access modifier (default to private if none specified)
def accessFlag = flag('access')
if (accessFlag != null) {
    try {
        field.accessModifier = FieldDefinition.AccessModifier.valueOf(accessFlag.toString().toUpperCase())
    } catch (IllegalArgumentException e) {
        error "Invalid access modifier: ${accessFlag}. Use: private, protected, or public"
        return false
    }
} else {
    // Default to private
    field.accessModifier = FieldDefinition.AccessModifier.PRIVATE
}

def nullableFlag = flag('nullable')
def notNullableFlag = flag('not-nullable')
if (nullableFlag != null) {
    field.nullable = true
} else if (notNullableFlag != null) {
    field.nullable = false
}

def blankFlag = flag('blank')
def notBlankFlag = flag('not-blank')
if (blankFlag != null) {
    field.blank = true
} else if (notBlankFlag != null) {
    field.blank = false
}

def maxSizeFlag = flag('max-size')
if (maxSizeFlag != null) {
    try {
        field.maxSize = maxSizeFlag instanceof Integer ? maxSizeFlag : Integer.parseInt(maxSizeFlag.toString())
    } catch (NumberFormatException e) {
        error "Invalid max-size value: ${maxSizeFlag}"
        return false
    }
}

def minSizeFlag = flag('min-size')
if (minSizeFlag != null) {
    try {
        field.minSize = minSizeFlag instanceof Integer ? minSizeFlag : Integer.parseInt(minSizeFlag.toString())
    } catch (NumberFormatException e) {
        error "Invalid min-size value: ${minSizeFlag}"
        return false
    }
}

def constraintStyleFlag = flag('constraint-style')
if (constraintStyleFlag != null) {
    try {
        field.constraintStyle = AbstractMemberDefinition.ConstraintStyle.valueOf(constraintStyleFlag.toString().toUpperCase())
    } catch (IllegalArgumentException e) {
        error "Invalid constraint-style value: ${constraintStyleFlag}. Use: grails, jakarta, or both"
        return false
    }
}

try {
    field.validate()
} catch (IllegalArgumentException e) {
    error "Invalid field definition: ${e.message}"
    return false
}

def sourceClass = source(domainClassName)
if (!sourceClass) {
    error "Domain class not found: ${domainClassName}"
    error "Run 'grails create-domain-class ${domainClassName}' first."
    return false
}

def domainFile = sourceClass.file
def modifier = new DomainFieldModifier()

if (modifier.memberExists(domainFile, field.name)) {
    error "Field '${field.name}' already exists in ${domainClassName}"
    return false
}

try {
    modifier.addField(domainFile, field)
    addStatus "Added field '${field.accessModifier} ${field.name}' to ${projectPath(sourceClass)}"
} catch (Exception e) {
    error "Failed to add field: ${e.message}"
    return false
}

return true
