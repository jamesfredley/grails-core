package grails.mongodb.bootstrap

import grails.gorm.annotation.Entity
import grails.mongodb.MongoEntity
import org.apache.grails.testing.AutoStartedMongoSpec
import org.grails.datastore.mapping.mongo.MongoDatastore
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by graemerocher on 05/10/2016.
 */
class EventsSetupSpec extends AutoStartedMongoSpec {

    @AutoCleanup
    @Shared
    MongoDatastore mongoDatastore

    void setupSpec() {
        mongoDatastore = new MongoDatastore(['grails.mongodb.host':mongoHost, 'grails.mongodb.port': mongoPort], MyEventSender)
    }

    void 'test events get triggered'() {
        setup:
        MyEventSender.DB.drop()
        when:
        new MyEventSender(name: "fred").save(flush:true)

        then:
        MyEventSender.first().name == 'FRED'
    }
}

@Entity
class MyEventSender implements MongoEntity<MyEventSender> {
    String name

    def beforeInsert() {
        name = name.toUpperCase()
    }
}
