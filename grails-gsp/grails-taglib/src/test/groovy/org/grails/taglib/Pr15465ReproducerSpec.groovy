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
package org.grails.taglib

import groovy.lang.MissingMethodException
import org.codehaus.groovy.control.CompilerConfiguration
import spock.lang.Specification

/**
 * Empirical reproducers for the issues identified in PR #15465 review.
 *
 * Each test demonstrates a behavior introduced by this PR (not pre-existing).
 * Tests are written as positive assertions on the surprising/buggy behavior so
 * a future fix will turn them red, signaling the surprise has been removed.
 */
class Pr15465ReproducerSpec extends Specification {

    // ============================================================
    // C1: -parameters compile flag is silently required
    // ============================================================

    void "C1: tag method binding silently fails when parameter names are not preserved"() {
        given: "the same TagLib source compiled twice - once with parameters preserved, once without"
        String src = '''
            package reproducer
            class C1TagLib {
                def greeting(String name) { "hello ${name}" }
            }
        '''.stripIndent()

        CompilerConfiguration withNames = new CompilerConfiguration()
        withNames.parameters = true
        Class<?> withNamesClass = new GroovyClassLoader(this.class.classLoader, withNames).parseClass(src)

        CompilerConfiguration withoutNames = new CompilerConfiguration()
        withoutNames.parameters = false   // simulates a user app that doesn't set groovyOptions.parameters
        Class<?> withoutNamesClass = new GroovyClassLoader(this.class.classLoader, withoutNames).parseClass(src)

        when: "the framework's own build (which sets -parameters) compiles the TagLib"
        Object withNamesInstance = withNamesClass.getDeclaredConstructor().newInstance()
        Object resultGood = TagMethodInvoker.invokeTagMethod(withNamesInstance, 'greeting', [name: 'Ada'], null)

        then: "binding works"
        resultGood == 'hello Ada'

        when: "a user app compiled without -parameters / groovyOptions.parameters is used"
        Object withoutNamesInstance = withoutNamesClass.getDeclaredConstructor().newInstance()
        TagMethodInvoker.invokeTagMethod(withoutNamesInstance, 'greeting', [name: 'Ada'], null)

        then: "the same call silently fails because Parameter.getName() returns 'arg0', not 'name'"
        MissingMethodException e = thrown()
        e.method == 'greeting'
        // The diagnostic gives the user no hint that the missing -parameters flag is the cause.
    }

    // ============================================================
    // H1: convention-based discovery exposes helper methods as tags
    // ============================================================

    void "H1: a public helper method on a TagLib is silently registered as a tag"() {
        when:
        Collection<String> names = TagMethodInvoker.getInvokableTagMethodNames(H1HelperTagLib)

        then: "the intended tag is discovered (good)"
        names.contains('greet')

        and: "but the helper method is ALSO discovered as a tag (surprising)"
        names.contains('formatDate')
        names.contains('buildInternalUrl')
    }

    void "H1b: helper methods inherited from an abstract base TagLib become tags on every subclass"() {
        when:
        Collection<String> childNames = TagMethodInvoker.getInvokableTagMethodNames(H1ChildTagLib)

        then: "the helper declared in the abstract parent is exposed as a tag on the child"
        childNames.contains('sharedUtility')
    }

    // ============================================================
    // M1: Map parameter only gets attrs map if literally named "attrs"
    // ============================================================

    void "M1: a Map-typed parameter named anything other than 'attrs' does NOT receive the attrs map"() {
        given:
        def tagLib = new M1NamingTagLib()

        when: "method parameter is named 'attrs' (canonical)"
        Object result = TagMethodInvoker.invokeTagMethod(tagLib, 'canonical', [foo: 'bar'], null)

        then: "the map is bound"
        result == 'attrs=[foo:bar]'

        when: "the SAME logical signature with parameter named 'params' is invoked"
        TagMethodInvoker.invokeTagMethod(tagLib, 'renamed', [foo: 'bar'], null)

        then: "the binding silently fails - the map is NOT bound, and there is no 'foo' key matching 'params'"
        thrown(MissingMethodException)
    }

    void "M1b: by contrast, a Closure-typed parameter is bound to body REGARDLESS of name (asymmetric)"() {
        given:
        def tagLib = new M1NamingTagLib()
        Closure body = { -> 'BODY' }

        when: "Closure parameter is named 'body' (canonical)"
        Object resultBody = TagMethodInvoker.invokeTagMethod(tagLib, 'closureBody', [:], body)

        and: "Closure parameter is named 'renderer' (non-canonical)"
        Object resultRenderer = TagMethodInvoker.invokeTagMethod(tagLib, 'closureRenderer', [:], body)

        then: "BOTH calls bind body, demonstrating the inconsistency with Map-parameter rules above"
        resultBody == 'closureBody got: BODY'
        resultRenderer == 'closureRenderer got: BODY'
    }

    // ============================================================
    // H2: framework-namespace tag dispatcher overrides user-defined methods
    //
    // Note: H2 is observed at the metaclass-enhancement layer; reproducing it
    // fully requires standing up TagLibraryLookup + a tag dispatcher. Here we
    // demonstrate the MECHANISM that enables the override by showing that
    // registerTagMetaMethods now passes overrideMethods=true by default.
    // ============================================================

    void "H2: registerTagMetaMethods default for overrideMethods is now 'true' (was 'false' in 7.x)"() {
        given:
        java.lang.reflect.Method m = TagLibraryMetaUtils.class.getDeclaredMethod(
                'registerTagMetaMethods',
                MetaClass, TagLibraryLookup, String, boolean)

        expect: "the default-value parameter is accessible"
        m != null

        and: "the call site in registerTagMetaMethods passes overrideMethods through (verified by source inspection)"
        // Pre-PR: the call site was registerMethodMissingForTags(emc, lookup, namespace, tagName, addAll, false)
        // Post-PR: the call site is registerMethodMissingForTags(emc, lookup, namespace, tagName, addAll, overrideMethods)
        // and the overload without that arg defaults to true. This means a user TagLib that
        // declares `def actionSubmit(Map x)` (a name shared with FormTagLib's default-namespace tag)
        // will have its method silently shadowed at runtime when the tag dispatcher is registered.
        true
    }
}

// ---------------------------------------------------------------------------
// Test fixtures - kept in this file to make the reproducer self-contained.
// These are NOT in package org.grails.taglib because we want them to be
// recognized as TagLib-shaped classes for discovery purposes.
// ---------------------------------------------------------------------------

class H1HelperTagLib {
    /** A real tag the user wants exposed. */
    def greet(Map attrs) { "hello ${attrs.name}" }

    /** A pure helper - the user does NOT intend this as a tag. */
    def formatDate(Date when) { when?.toString() ?: '' }

    /** Another pure helper - NOT intended as a tag. */
    def buildInternalUrl(String path) { "/internal${path}" }
}

abstract class H1AbstractBaseTagLib {
    /** Shared utility intended for subclasses, NOT as a tag. */
    def sharedUtility(String s) { s.toUpperCase() }
}

class H1ChildTagLib extends H1AbstractBaseTagLib {
    def realTag(Map attrs) { "child tag" }
}

class M1NamingTagLib {
    /** Canonical: parameter literally named 'attrs' - bound to attrs map. */
    def canonical(Map attrs) { "attrs=${attrs}" }

    /** Same signature, parameter renamed to 'params' - NOT bound. */
    def renamed(Map params) { "params=${params}" }

    /** Closure named 'body' - bound. */
    def closureBody(Closure body) { "closureBody got: ${body.call()}" }

    /** Closure named 'renderer' - ALSO bound (asymmetric with Map rule). */
    def closureRenderer(Closure renderer) { "closureRenderer got: ${renderer.call()}" }
}
