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
package org.grails.orm.hibernate.connections

import org.hibernate.dialect.H2Dialect
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import grails.gorm.annotation.Entity
import grails.gorm.services.Service
import grails.gorm.transactions.Transactional
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.orm.hibernate.HibernateDatastore

class DataServiceDatasourceInheritanceSpec extends Specification {

    @Shared Map config = [
            'dataSource.url':"jdbc:h2:mem:grailsDB;LOCK_TIMEOUT=10000",
            'dataSource.dbCreate': 'create-drop',
            'dataSource.dialect': H2Dialect.name,
            'dataSource.formatSql': 'true',
            'hibernate.flush.mode': 'COMMIT',
            'hibernate.cache.queries': 'true',
            'hibernate.hbm2ddl.auto': 'create-drop',
            'dataSources.warehouse':[url:"jdbc:h2:mem:warehouseDB;LOCK_TIMEOUT=10000"],
    ]

    @Shared @AutoCleanup HibernateDatastore datastore = new HibernateDatastore(
            DatastoreUtils.createPropertyResolver(config), Inventory
    )

    @Shared InventoryService inventoryService
    @Shared InventoryDataService inventoryDataService
    @Shared ExplicitInventoryService explicitInventoryService

    void setupSpec() {
        inventoryService = datastore
                .getDatastoreForConnection('warehouse')
                .getService(InventoryService)
        inventoryDataService = datastore
                .getDatastoreForConnection('warehouse')
                .getService(InventoryDataService)
        explicitInventoryService = datastore
                .getDatastoreForConnection('warehouse')
                .getService(ExplicitInventoryService)
    }

    void setup() {
        def api = GormEnhancer.findStaticApi(Inventory, 'warehouse')
        api.withNewTransaction {
            api.executeUpdate('delete from Inventory')
        }
    }

    void "abstract service without @Transactional(connection) inherits from domain"() {
        when: "saving through a service that has no @Transactional(connection)"
        def saved = inventoryService.save(new Inventory(sku: 'ABC-001', quantity: 50))

        then: "the entity is persisted on the warehouse datasource"
        saved != null
        saved.id != null
        saved.sku == 'ABC-001'

        and: "it exists on the warehouse datasource"
        GormEnhancer.findStaticApi(Inventory, 'warehouse').withNewTransaction {
            GormEnhancer.findStaticApi(Inventory, 'warehouse').count()
        } == 1
    }

    void "get by ID routes to inherited datasource"() {
        given: "an inventory item saved on warehouse"
        def saved = inventoryService.save(new Inventory(sku: 'GET-001', quantity: 10))

        when: "retrieving by ID"
        def found = inventoryService.get(saved.id)

        then: "the correct entity is returned"
        found != null
        found.id == saved.id
        found.sku == 'GET-001'
    }

    void "delete routes to inherited datasource"() {
        given: "an inventory item saved on warehouse"
        def saved = inventoryService.save(new Inventory(sku: 'DEL-001', quantity: 5))

        when: "deleting by ID"
        def deleted = inventoryService.delete(saved.id)

        then: "the entity is deleted"
        deleted != null
        deleted.sku == 'DEL-001'
        inventoryService.get(saved.id) == null
    }

    void "count routes to inherited datasource"() {
        given: "items saved on warehouse"
        inventoryService.save(new Inventory(sku: 'CNT-001', quantity: 1))
        inventoryService.save(new Inventory(sku: 'CNT-002', quantity: 2))

        expect: "count returns 2"
        inventoryService.count() == 2
    }

    void "findBySku routes to inherited datasource"() {
        given: "items saved on warehouse"
        inventoryService.save(new Inventory(sku: 'FIND-001', quantity: 100))

        when: "finding by sku"
        def found = inventoryService.findBySku('FIND-001')

        then: "the correct entity is returned"
        found != null
        found.sku == 'FIND-001'
        found.quantity == 100
    }

    void "interface service inherits datasource from domain"() {
        when: "saving through an interface service with no @Transactional(connection)"
        def saved = inventoryDataService.save(new Inventory(sku: 'IFACE-001', quantity: 25))

        then: "the entity is persisted on warehouse"
        saved != null
        saved.id != null

        and: "retrievable through the same service"
        inventoryDataService.get(saved.id) != null
    }

    void "explicit @Transactional(connection) wins over domain datasource"() {
        when: "saving through a service with explicit @Transactional(connection='warehouse')"
        def saved = explicitInventoryService.save(new Inventory(sku: 'EXPL-001', quantity: 75))

        then: "the entity is persisted correctly"
        saved != null
        saved.id != null
        saved.sku == 'EXPL-001'
    }

    void "abstract and interface services share the same inherited datasource"() {
        given: "an item saved through the abstract service"
        def saved = inventoryService.save(new Inventory(sku: 'CROSS-001', quantity: 42))

        expect: "the interface service can find it"
        inventoryDataService.findBySku('CROSS-001') != null
        inventoryDataService.findBySku('CROSS-001').id == saved.id
    }

}

@Entity
class Inventory {
    Long id
    Long version
    String sku
    Integer quantity

    static mapping = {
        datasource 'warehouse'
    }
    static constraints = {
        sku blank: false
    }
}

@Service(Inventory)
abstract class InventoryService {

    abstract Inventory get(Serializable id)

    abstract Inventory save(Inventory item)

    abstract Inventory delete(Serializable id)

    abstract Number count()

    abstract Inventory findBySku(String sku)
}

@Service(Inventory)
interface InventoryDataService {

    Inventory get(Serializable id)

    Inventory save(Inventory item)

    Inventory delete(Serializable id)

    Inventory findBySku(String sku)
}

@Service(Inventory)
@Transactional(connection = 'warehouse')
abstract class ExplicitInventoryService {

    abstract Inventory save(Inventory item)
}
