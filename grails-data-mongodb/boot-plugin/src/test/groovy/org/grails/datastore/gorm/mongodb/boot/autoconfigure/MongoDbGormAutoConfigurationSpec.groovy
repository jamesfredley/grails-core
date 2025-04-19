package org.grails.datastore.gorm.mongodb.boot.autoconfigure

import grails.gorm.annotation.Entity
import org.apache.grails.testing.AbstractMongoGrailsExtension
import org.apache.grails.testing.AutoStartedMongoSpec
import org.springframework.boot.autoconfigure.AutoConfigurationPackages
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import spock.lang.Specification

/**
 * Tests for MongoDB autoconfigure
 */
class MongoDbGormAutoConfigurationSpec extends AutoStartedMongoSpec {

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
        when:"The context is refreshed"
            context.refresh()

        then:"GORM queries work"
            Person.count() != null
    }

    @Configuration
    @Import(MongoDbGormAutoConfiguration)
    static class TestConfiguration {
    }
}

@Entity
class Person {
    String firstName
    String lastName
    Integer age = 18
}
