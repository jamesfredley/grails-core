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

package functional.tests

import functional.tests.pages.BookListPage
import functional.tests.pages.BookShowPage
import grails.plugin.geb.ContainerGebSpec
import grails.testing.mixin.integration.Integration

@Integration(applicationClass = Application)
class BookControllerSpec extends ContainerGebSpec {

    void "Test list books"() {
        when:"The home page is visited"
        to(BookListPage)

        then:"The title is correct"
        at(BookListPage)
    }

    void "Test save book"() {
        when:
        go "/book/create"
        $('form').title = "The Stand"
        $('input.save').click()

        then:"The book is correct"
        waitFor { title == BookShowPage.pageTitle }
        $('li.fieldcontain div').text() == 'The Stand'
    }
}
