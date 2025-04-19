package example

import org.grails.datastore.mapping.mongo.MongoDatastore
import org.grails.datastore.mapping.mongo.config.MongoSettings
import org.slf4j.LoggerFactory
import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.model.FieldInfo
import org.spockframework.runtime.model.SpecInfo
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.utility.DockerImageName

class SpringBootStartMongoExtension implements IGlobalExtension {
    static GenericContainer dbContainer

    @Override
    void start() {
        String mongoVersion = System.getProperty("mongodbContainerVersion", "7.0.19")
        DockerImageName dockerImage = DockerImageName.parse("mongo:${mongoVersion}")
        dbContainer = new MongoDBContainer(dockerImage)
        dbContainer.start()
        dbContainer.followOutput(new Slf4jLogConsumer(LoggerFactory.getLogger("testcontainers")))
    }

    @Override
    void visitSpec(SpecInfo spec) {
        spec.addSharedInitializerInterceptor { invocation ->
            FieldInfo mongoDatastoreField = spec.fields.find { it.shared && MongoDatastore.isAssignableFrom(it.type) }
            if(mongoDatastoreField) {
                mongoDatastoreField.writeValue(invocation.sharedInstance, new MongoDatastore([(MongoSettings.SETTING_HOST): dbContainer.getHost(), (MongoSettings.SETTING_PORT): dbContainer.getMappedPort(27017) as String], getClass().getPackage()))
            }
        }
    }

    @Override
    void stop() {
        if(dbContainer) {
            dbContainer.start()
        }
    }
}
