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
package grails.gorm.tests

import org.apache.grails.data.testing.tck.base.GrailsDataTckSpec
import org.apache.grails.data.testing.tck.domains.ChildEntity
import org.apache.grails.data.testing.tck.domains.TestEntity
import org.apache.grails.data.simple.core.GrailsDataCoreTckManager

/**
 * Created by graemerocher on 16/02/2017.
 */
class DeepValidateWithSaveSpec extends GrailsDataTckSpec<GrailsDataCoreTckManager> {

    void "save with deepValidate: true succeeds for a valid entity"() {
        given: "a valid TestEntity"
        def entity = new TestEntity(name: 'testDeepValidate', age: 10, child: new ChildEntity(name: 'child'))

        when: "saved with deepValidate: true"
        def saved = entity.save(deepValidate: true, flush: true)

        then: "the entity is persisted without errors"
        saved != null
        saved.id != null
        !saved.hasErrors()
    }

    void "save with deepValidate: false still saves a valid entity"() {
        given: "a valid TestEntity"
        def entity = new TestEntity(name: 'testShallowValidate', age: 10, child: new ChildEntity(name: 'child'))

        when: "saved with deepValidate: false"
        def saved = entity.save(deepValidate: false, flush: true)

        then: "the entity is persisted without errors"
        saved != null
        saved.id != null
        !saved.hasErrors()
    }
}
