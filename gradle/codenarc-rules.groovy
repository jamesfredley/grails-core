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

/**
 * Centralized CodeNarc rules for detecting unused code and dead files
 * Focuses on comprehensive detection of unused elements across Grails projects
 */

ruleset {

    // Core unused code detection rules
    // rulesets/unused.xml
    UnusedArray
    UnusedObject
    UnusedPrivateField
    UnusedPrivateMethod
    UnusedPrivateMethodParameter
    UnusedVariable
    
    // Enable unused method parameter detection but exclude test methods
    'UnusedMethodParameter' {
        enabled = true
        doNotApplyToFilesMatching = '.*Spec.groovy'
    }

    // Dead code detection
    // rulesets/basic.xml
    DeadCode
    
    // Empty and unnecessary constructs that indicate potential dead code
    'EmptyClass' doNotApplyToFilesMatching: '.*Spec.groovy'
    EmptyMethod
    EmptyElseBlock
    EmptyFinallyBlock
    EmptyForStatement
    EmptyIfStatement
    EmptyInstanceInitializer
    EmptyStaticInitializer
    EmptySwitchStatement
    EmptySynchronizedStatement
    EmptyTryBlock
    EmptyWhileStatement

    // Unnecessary code that may indicate unused or dead code
    UnnecessaryOverridingMethod
    UnnecessaryReturnKeyword
    UnnecessaryPublicModifier
    UnnecessaryElseStatement
    UnnecessaryConstructor
    UnnecessaryDefInFieldDeclaration
    UnnecessaryDefInMethodDeclaration
    UnnecessaryDefInVariableDeclaration
    UnnecessaryPackageReference
    UnnecessaryGetter
    UnnecessaryInstantiationToGetClass
    UnnecessaryToString
    UnnecessaryTransientModifier

    // Import-related unused code detection
    UnusedImport
    UnnecessaryGroovyImport
    DuplicateImport
    ImportFromSamePackage

    // Design rules that help identify potentially unused code
    'AbstractClassWithoutAbstractMethod' enabled: true
    'ConstantsOnlyInterface' enabled: true
    EmptyMethodInAbstractClass
    
    // Exception handling that might indicate dead code paths
    'EmptyCatchBlock' enabled: true
    CatchException
    ThrowException
    
    // Security-related unused code
    UnsafeArrayDeclaration
    ObjectFinalize
    PublicFinalizeMethod

    // Logging rules - unused loggers
    LoggerForDifferentClass
    LoggerWithWrongModifiers
    MultipleLoggers

    // Naming rules that help identify inconsistencies suggesting unused code
    ClassNameSameAsFilename
    'MethodName' doNotApplyToFilesMatching: '.*Spec.groovy'
    FieldName
    VariableName
    PropertyName

    // Duplicate code detection which often indicates unused variations
    'DuplicateListLiteral' doNotApplyToFilesMatching: '.*Spec.groovy'
    'DuplicateMapLiteral' doNotApplyToFilesMatching: '.*Spec.groovy'
    DuplicateCaseStatement
    DuplicateMapKey
    DuplicateSetValue

    // Test-specific unused code rules
    JUnitUnnecessarySetUp
    JUnitUnnecessaryTearDown
    JUnitUnnecessaryThrowsException
    UnnecessaryFail

    // Size and complexity rules to identify overly complex unused code
    ClassSize
    MethodCount
    'ParameterCount' maxParameters: 6

    // Performance-related unused code
    'Instanceof' enabled: false  // Can be noisy but sometimes indicates unused casting
    UnnecessaryInstanceOfCheck
    UnnecessaryNullCheck
    UnnecessaryNullCheckBeforeInstanceOf

    // Generic rules for identifying unused code patterns
    IllegalClassMember
    IllegalPackageReference
    StatelessClass

    // Exclude common patterns that are not actually unused
    excludes = [
        '**/test/**/*Spec.groovy',
        '**/src/test/**',
        '**/*Test.groovy',
        '**/*Tests.groovy'
    ]
}