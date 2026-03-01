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
package org.grails.web.taglib

import grails.testing.web.taglib.TagLibUnitTest
import org.grails.plugins.web.taglib.FormatTagLib
import spock.lang.IgnoreIf
import spock.lang.Issue
import spock.lang.Requires
import spock.lang.Specification

class FormatTagLibSpec extends Specification implements TagLibUnitTest<FormatTagLib> {

    @Requires({ jvm.isJava8() })
    void testFormatCurrencyForJava8() {
        given:
        BigDecimal number = "3.12325678" as BigDecimal
        "3,12 €" == applyTemplate('<g:formatNumber type="currency" number="${number}" locale="fi_FI" />', [number: number])
    }

    @IgnoreIf({ jvm.isJava8() })
    void testFormatCurrency() {
        given:
        BigDecimal number = "3.12325678" as BigDecimal
        "3,12${new String([160] as char[])}€" == applyTemplate('<g:formatNumber type="currency" number="${number}" locale="fi_FI" />', [number: number])
    }

    @Requires({ jvm.isJava8() })
    void testFormatCurrencyWithCodeAndLocaleForJava8() {
        given:
        BigDecimal number = "3.12325678" as BigDecimal

        expect:
        "3,12 USD" == applyTemplate('<g:formatNumber type="currency" currencyCode="USD" number="${number}" locale="fi_FI" />',  [number: number])
    }

    @IgnoreIf({ jvm.isJava8() })
    void testFormatCurrencyWithCodeAndLocale() {
        given:
        BigDecimal number = "3.12325678" as BigDecimal

        expect:
        "3,12${new String([160] as char[])}\$" == applyTemplate('<g:formatNumber type="currency" currencyCode="USD" number="${number}" locale="fi_FI" />',  [number: number])
    }

    @Issue('https://github.com/apache/grails-core/issues/15178')
    void "formatNumber uses ASCII minus sign for negative numbers with Norwegian locale"() {
        when:
        def result = applyTemplate('<g:formatNumber number="${n}" type="number" locale="nb_NO"/>', [n: -42])

        then: "ASCII minus (U+002D) is used, not Unicode minus (U+2212)"
        result.contains('-')
        !result.contains('\u2212')
    }
}
