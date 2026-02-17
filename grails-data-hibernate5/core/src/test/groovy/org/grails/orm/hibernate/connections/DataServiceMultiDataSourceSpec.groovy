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

import grails.gorm.services.Service
import grails.gorm.annotation.Entity
import grails.gorm.transactions.Transactional
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.orm.hibernate.HibernateDatastore
import org.hibernate.dialect.H2Dialect
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

/**
 * Integration tests for GORM Data Service auto-implemented CRUD methods
 * routing to a non-default datasource via @Transactional(connection).
 *
 * The Product domain is mapped exclusively to the 'books' datasource.
 * Without the connection-routing fix, auto-implemented save/get/delete
 * would attempt to use the default datasource (where no Product table
 * exists), causing failures.
 *
 * Tests both patterns:
 * - Abstract class implementing interface (ProductService)
 * - Interface-only with @Transactional(connection) (ProductDataService)
 *
 * @see org.grails.datastore.gorm.services.implementers.SaveImplementer
 * @see org.grails.datastore.gorm.services.implementers.DeleteImplementer
 * @see org.grails.datastore.gorm.services.implementers.FindAndDeleteImplementer
 * @see org.grails.datastore.gorm.services.implementers.AbstractDetachedCriteriaServiceImplementor
 */
class DataServiceMultiDataSourceSpec extends Specification {

    @Shared Map config = [
            'dataSource.url':"jdbc:h2:mem:grailsDB;LOCK_TIMEOUT=10000",
            'dataSource.dbCreate': 'create-drop',
            'dataSource.dialect': H2Dialect.name,
            'dataSource.formatSql': 'true',
            'hibernate.flush.mode': 'COMMIT',
            'hibernate.cache.queries': 'true',
            'hibernate.hbm2ddl.auto': 'create-drop',
            'dataSources.books':[url:"jdbc:h2:mem:booksDB;LOCK_TIMEOUT=10000"],
    ]

    @Shared @AutoCleanup HibernateDatastore datastore = new HibernateDatastore(
            DatastoreUtils.createPropertyResolver(config), Product
    )

    @Shared ProductService productService

    void setupSpec() {
        productService = datastore.getDatastoreForConnection("books").getService(ProductService)
    }

    void setup() {
        GormStaticApi<Product> api = GormEnhancer.findStaticApi(Product, 'books')
        api.withNewTransaction {
            api.executeUpdate('delete from Product')
        }
    }

    void "schema is created on the books datasource"() {
        when: "we query the books datasource for the product table"
        GormStaticApi<Product> api = GormEnhancer.findStaticApi(Product, 'books')
        List result = api.withNewTransaction {
            api.executeQuery("SELECT 1 FROM Product p WHERE 1=0")
        }

        then: "no exception - table exists on books"
        noExceptionThrown()
        result != null
    }

    void "save routes to books datasource"() {
        when: "a product is saved through the Data Service"
        Product saved = productService.save(new Product(name: 'Widget', amount: 42))

        then: "it is persisted with an ID"
        saved != null
        saved.id != null
        saved.name == 'Widget'
        saved.amount == 42

        and: "it exists on the books datasource"
        GormEnhancer.findStaticApi(Product, 'books').withNewTransaction {
            GormEnhancer.findStaticApi(Product, 'books').count()
        } == 1
    }

    void "get by ID routes to books datasource"() {
        given: "a product saved on books"
        Product saved = productService.save(new Product(name: 'Gadget', amount: 99))

        when: "we retrieve it by ID"
        Product found = productService.get(saved.id)

        then: "the correct entity is returned"
        found != null
        found.id == saved.id
        found.name == 'Gadget'
        found.amount == 99
    }

    void "count routes to books datasource"() {
        given: "two products saved on books"
        productService.save(new Product(name: 'Alpha', amount: 10))
        productService.save(new Product(name: 'Beta', amount: 20))

        expect: "count returns 2"
        productService.count() == 2
    }

    void "delete by ID routes to books datasource - FindAndDeleteImplementer"() {
        given: "a product saved on books"
        Product saved = productService.save(new Product(name: 'Ephemeral', amount: 1))

        when: "we delete it using delete(id) which returns the domain object"
        Product deleted = productService.delete(saved.id)

        then: "the deleted entity is returned and no longer exists"
        deleted != null
        deleted.name == 'Ephemeral'
        productService.get(saved.id) == null
        productService.count() == 0
    }

    void "delete by ID routes to books datasource - DeleteImplementer"() {
        given: "a product saved on books"
        Product saved = productService.save(new Product(name: 'AlsoEphemeral', amount: 2))

        when: "we delete it using void deleteProduct(id)"
        productService.deleteProduct(saved.id)

        then: "it no longer exists"
        productService.get(saved.id) == null
        productService.count() == 0
    }

    void "findByName routes to books datasource"() {
        given: "products saved on books"
        productService.save(new Product(name: 'Unique', amount: 77))
        productService.save(new Product(name: 'Other', amount: 88))

        when: "we find by name"
        Product found = productService.findByName('Unique')

        then: "the correct entity is returned"
        found != null
        found.name == 'Unique'
        found.amount == 77
    }

    void "findAllByName routes to books datasource"() {
        given: "products with duplicate names on books"
        productService.save(new Product(name: 'Duplicate', amount: 10))
        productService.save(new Product(name: 'Duplicate', amount: 20))
        productService.save(new Product(name: 'Singleton', amount: 30))

        when: "we find all by name"
        List<Product> found = productService.findAllByName('Duplicate')

        then: "both matching entities are returned"
        found.size() == 2
        found.every { it.name == 'Duplicate' }
    }

    void "GormEnhancer escape-hatch HQL works on books datasource"() {
        given: "products saved on books"
        productService.save(new Product(name: 'Foo', amount: 100))
        productService.save(new Product(name: 'Bar', amount: 200))

        when: "we run aggregate HQL through GormEnhancer"
        GormStaticApi<Product> api = GormEnhancer.findStaticApi(Product, 'books')
        List result = api.withNewTransaction {
            api.executeQuery("SELECT SUM(p.amount) FROM Product p")
        }

        then: "the aggregation reflects books data"
        result[0] == 300
    }

    void "save, get, and find round-trip through Data Service"() {
        when: "a product is saved, retrieved by ID, and found by name"
        Product saved = productService.save(new Product(name: 'RoundTrip', amount: 33))
        Product byId = productService.get(saved.id)
        Product byName = productService.findByName('RoundTrip')

        then: "all three references point to the same entity"
        saved.id == byId.id
        saved.id == byName.id
        byId.name == 'RoundTrip'
        byName.amount == 33
    }

    void "save with constructor-style arguments routes to books datasource"() {
        when: "a product is saved using property arguments"
        Product saved = productService.saveProduct('Constructed', 55)

        then: "it is persisted on books"
        saved != null
        saved.id != null
        saved.name == 'Constructed'
        saved.amount == 55

        and: "retrievable"
        productService.get(saved.id) != null
    }
}

@Entity
class Product {
    Long id
    Long version
    String name
    Integer amount

    static mapping = {
        datasource 'books'
    }
    static constraints = {
        name blank: false
    }
}

@Service(Product)
@Transactional(connection = 'books')
abstract class ProductService {

    abstract Product get(Serializable id)

    abstract Product save(Product product)

    abstract Product delete(Serializable id)

    abstract void deleteProduct(Serializable id)

    abstract Number count()

    abstract Product findByName(String name)

    abstract List<Product> findAllByName(String name)

    /**
     * Constructor-style save - GORM creates the entity from parameters.
     * Tests that SaveImplementer routes multi-arg saves through connection-aware API.
     */
    abstract Product saveProduct(String name, Integer amount)
}
