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

package functionaltests

import functionaltests.pages.BookCreatePage
import functionaltests.pages.BookListPage
import functionaltests.pages.BookShowPage
import grails.plugin.geb.ContainerGebSpec
import grails.testing.mixin.integration.Integration
import spock.lang.Issue

@Integration(applicationClass = functionaltests.Application)
class BookFunctionalSpec extends ContainerGebSpec {

    void "Test that when the /viewBooks URL is hit it redirects to the book list"() {
        when: "We go to the book URI"
        to(BookListPage)

        then: "Then thew show book view is rendered"
        at(BookListPage)
    }

    void "Test that a book was created in the Bootstrap class"() {
        when: "We go to the book URI"
        to(BookShowPage, 1)

        then: "Then thew show book view is rendered"
        at(BookShowPage)
    }

    void "Test that switching language results in correct encodings"() {
        when: "the show page is rendered in german"
        go "/book/show/1?lang=de"
        then: "The language is correct"
        $('a', class: 'create').text() == 'Book anlegen'
        $('input', class: 'delete').@value == 'Löschen'
    }

    @Issue('10965')
    void "When creating a book the params are not on the url"() {
        when: 'creating a book'
        to(BookCreatePage)
        $('#title').value('The Stand')
        $('#create').click()

        then:
        waitFor { title == BookShowPage.pageTitle }
        !currentUrl.contains('title')
        !currentUrl.contains('create')
    }
}
