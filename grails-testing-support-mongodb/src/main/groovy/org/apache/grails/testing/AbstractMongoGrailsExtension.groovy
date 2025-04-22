package org.apache.grails.testing

import org.spockframework.runtime.model.SpecInfo
import org.testcontainers.utility.DockerImageName

/**
 * Abstract class for Mongo Grails extensions.
 *
 * @since 7.0.0
 * @author James Daugherty
 */
abstract class AbstractMongoGrailsExtension {
    final static int DEFAULT_MONGO_PORT = 27017
    final static String DEFAULT_MONGO_VERSION = '7.0.19'
    final static String MONGO_VERSION_PROPERTY = 'mongodbContainerVersion'

    /**
     * Returns the desire mongo version. If not set, it will by defaulted to `DEFAULT_MONGO_VERSION`.
     */
    static String getDesiredMongoVersion() {
        System.getProperty(MONGO_VERSION_PROPERTY, DEFAULT_MONGO_VERSION)
    }

    /**
     * Returns the configured docker image name
     */
    static DockerImageName getDesiredMongoDockerName() {
        DockerImageName.parse("mongo:${getDesiredMongoVersion()}")
    }

    /**
     * Integration tests have a special property added by the Grails Gradle plugin that ensure they can be detected
     * so different extensions can be applied on Unit vs Integration
     */
    boolean isIntegrationTestRun() {
        Boolean.getBoolean('is.grails.integration.test') as boolean
    }

    /**
     * MongoSpec is used to spin up mongo db instances per a test and let the end-user control when it's started.
     */
    boolean isMongoSpec(SpecInfo spec) {
        try {
            Class<?> mongoSpec = Class.forName('grails.test.mongodb.MongoSpec')
            return mongoSpec.isAssignableFrom(spec.getReflection())
        } catch (ClassNotFoundException e) {
            return false
        }
    }

    /**
     * AutoStartedMongoSpec is the marker class that enables our extension to autostart mongodb in a container
     */
    boolean isAutoStartedMongoSpec(SpecInfo spec) {
        try {
            Class<?> mongoSpec = Class.forName('org.apache.grails.testing.AutoStartedMongoSpec')
            return mongoSpec.isAssignableFrom(spec.getReflection())
        } catch (ClassNotFoundException e) {
            return false
        }
    }

    /**
     * Determines if MongoDB is already running.  In the event that it is, extensions will not override it.
     */
    boolean isMongoAlreadyRunning() {
        try (Socket ignored = new Socket('localhost', DEFAULT_MONGO_PORT)) {
            return true
        } catch (IOException ex) {
            return false
        }
    }

    boolean isIntegrationSpec(SpecInfo spec) {
        try {
            Class<?> integrationSpec = Class.forName('grails.testing.mixin.integration.Integration')
            return spec.annotations.any { integrationSpec.isAssignableFrom(it.annotationType()) }
        } catch (ClassNotFoundException e) {
            return false
        }
    }

    protected String createConnectionString(String host, int port) {
        "mongodb://${host}:${port as String}/myDb" as String
    }
}
