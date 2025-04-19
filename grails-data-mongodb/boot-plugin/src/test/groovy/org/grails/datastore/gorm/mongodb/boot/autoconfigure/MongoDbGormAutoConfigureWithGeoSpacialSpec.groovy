package org.grails.datastore.gorm.mongodb.boot.autoconfigure

import grails.gorm.annotation.Entity
import grails.mongodb.geo.Point
import org.apache.grails.testing.AbstractMongoGrailsExtension
import org.apache.grails.testing.AutoStartedMongoSpec
import org.bson.types.ObjectId
import org.springframework.boot.autoconfigure.AutoConfigurationPackages
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import spock.lang.Specification

/**
 * Created by graemerocher on 20/03/14.
 */
class MongoDbGormAutoConfigureWithGeoSpacialSpec extends AutoStartedMongoSpec {

    protected AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    @Override
    boolean shouldInitializeDatastore() {
        false
    }

    void setupSpec() {
        System.setProperty('spring.data.mongodb.host', dbContainer.getHost())
        System.setProperty('spring.data.mongodb.port', dbContainer.getMappedPort(AbstractMongoGrailsExtension.DEFAULT_MONGO_PORT) as String)
    }

    void cleanup() {
        context.close()
    }

    void setup() {
        AutoConfigurationPackages.register(context, "org.grails.datastore.gorm.mongodb.boot.autoconfigure")

        this.context.register(TestConfiguration, MongoAutoConfiguration.class,
                              PropertyPlaceholderAutoConfiguration.class);
    }

    void 'Test that GORM is correctly configured'() {

        given:"The context is refreshed"
            context.refresh()


        when:"Geospacial data is saved"
            City city
            def location = Point.valueOf([-0.125487, 51.508515])
            City.withTransaction {

                city = new City(name:"London", location: location)
                city.save(flush:true)
                city.discard()

                city = City.get(city.id)
                then:"GORM queries work"
            }
        then:
            city != null
            city.location == location
            City.findByLocationNear(location)
    }

    @Configuration
    @Import(MongoDbGormAutoConfiguration)
    static class TestConfiguration {
    }
}

@Entity
class City {
    ObjectId id
    String name
    Point location

    static constraints = {
        name blank:false
        location nullable:false
    }

    static mapping = {
        location geoIndex:'2dsphere'
    }
}
