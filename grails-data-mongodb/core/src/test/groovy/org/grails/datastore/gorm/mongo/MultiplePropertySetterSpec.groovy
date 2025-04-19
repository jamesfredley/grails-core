package org.grails.datastore.gorm.mongo

import grails.persistence.Entity
import org.apache.grails.testing.AutoStartedMongoSpec
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.grails.datastore.mapping.mongo.config.MongoSettings
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class MultiplePropertySetterSpec extends AutoStartedMongoSpec {
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
        ], Car)
    }

    void "test domain with multiple property setter"() {
        setup:
        new Car(name: "Ford EcoSport").save(flush: true, failOnError: true)

        expect:
        Car.count() == 1
    }

}

@Entity
class Car implements Serializable {

    Long id
    Long version

    String name

    void setId(Long id) {
        this.id = id
    }

    void setId(String id) {
        this.id = Long.parseLong(id)
    }
}
