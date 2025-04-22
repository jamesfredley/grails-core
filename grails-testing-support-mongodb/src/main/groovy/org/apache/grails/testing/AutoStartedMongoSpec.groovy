package org.apache.grails.testing

import org.testcontainers.containers.MongoDBContainer
import spock.lang.Shared
import spock.lang.Specification

/**
 * A base specification class to handling autostarting MongoDB & bootstrapping the mongo datastore if defined.
 * This class may only be used with unit tests.
 *
 * @since 7.0.0
 * @author James Daugherty
 */
abstract class AutoStartedMongoSpec extends Specification {

    @Shared
    String mongoHost

    @Shared
    String mongoPort

    @Shared
    MongoDBContainer dbContainer

    /**
     * If MongoDatastore is defined, it will be initialized with the dbContainer
     */
    boolean shouldInitializeDatastore() {
        return true
    }

    /**
     * The packages to initialize the MongoDatastore (if defined)
     */
    List<Package> getMongoPackages() {
        [getClass().getPackage()]
    }

    // Optionally declare a variable with type `MongoDatastore` and it will be initialized using the dbContainer
    // the the dBContainer connection for domains in the same package as this specification
}
