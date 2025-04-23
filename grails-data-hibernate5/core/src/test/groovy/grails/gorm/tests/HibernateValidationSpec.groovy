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

import grails.gorm.tck.ChildEntity
import grails.gorm.tck.ClassWithListArgBeforeValidate
import grails.gorm.tck.ClassWithNoArgBeforeValidate
import grails.gorm.tck.ClassWithOverloadedBeforeValidate
import grails.gorm.tck.TestEntity
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * Tests validation semantics.
 */
class HibernateValidationSpec extends GormDatastoreSpec {

    @Override
    List getDomainClasses() {
        return [ClassWithListArgBeforeValidate, ClassWithNoArgBeforeValidate,
                ClassWithOverloadedBeforeValidate]
    }

    void "Test that validate works without a bound Session"() {
        given:
        def t

        when:
        session.disconnect()
        def resource
        if (TransactionSynchronizationManager.hasResource(session.datastore.sessionFactory)) {
            resource = TransactionSynchronizationManager.unbindResource(session.datastore.sessionFactory)
        }

        t = new TestEntity(name:"")

        then:
        TransactionSynchronizationManager.getResource(session.datastore.sessionFactory) == null
        t.save() == null
        t.hasErrors() == true

        when:
        TransactionSynchronizationManager.bindResource(session.datastore.sessionFactory, resource)

        then:
        1 == t.errors.allErrors.size()
        0 == TestEntity.count()

        when:
        t.clearErrors()
        t.name = "Bob"
        t.age = 45
        t.child = new ChildEntity(name:"Fred")
        t = t.save(flush: true)

        then:
        t != null
        1 == TestEntity.count()
    }
}
