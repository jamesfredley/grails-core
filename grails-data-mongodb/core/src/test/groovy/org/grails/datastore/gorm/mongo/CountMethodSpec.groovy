package org.grails.datastore.gorm.mongo

import grails.gorm.annotation.Entity
import grails.mongodb.MongoEntity
import org.apache.grails.testing.AutoStartedMongoSpec
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.grails.datastore.mapping.mongo.config.MongoSettings
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import static com.mongodb.client.model.Filters.*

/**
 * Created by graemerocher on 29/11/2016.
 */
class CountMethodSpec extends AutoStartedMongoSpec {

    @Shared
    @AutoCleanup
    MongoDatastore mongoDatastore

    @Override
    boolean shouldInitializeDatastore() {
        false
    }

    void setupSpec() {
        mongoDatastore = new MongoDatastore([
                (MongoSettings.SETTING_HOST): mongoHost,
                (MongoSettings.SETTING_PORT): mongoPort,
        ], CountTest)
    }

    void "test count method"() {
        given: "some test data "
        CountTest.DB.drop()
        CountTest.withNewSession {
            new CountTest(name: "foo").save()
            new CountTest(name: "bar").save(flush: true)
        }

        expect:
        CountTest.find(eq("name", "foo"))
        CountTest.count(eq("name", "foo")) == 1
    }
}

@Entity
class CountTest implements MongoEntity<CountTest> {
    String name

}
