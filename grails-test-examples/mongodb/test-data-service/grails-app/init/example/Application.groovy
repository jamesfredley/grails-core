package example

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import groovy.transform.CompileStatic
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.utility.DockerImageName

@CompileStatic
class Application extends GrailsAutoConfiguration {
    static GenericContainer dbContainer = null

    static void main(String[] args) {
        String mongoVersion = System.getProperty("mongodbContainerVersion", "7.0.19")
        DockerImageName dockerImage = DockerImageName.parse("mongo:${mongoVersion}")
        dbContainer = new MongoDBContainer(dockerImage)
        dbContainer.start()
        dbContainer.followOutput(new Slf4jLogConsumer(LoggerFactory.getLogger("testcontainers")))

        System.setProperty("docker.mongodb.port", dbContainer.getMappedPort(27017).toString())
        System.setProperty("docker.mongodb.hostname", dbContainer.getHost())

        GrailsApp.run(Application, args)
    }

    @Override
    void onShutdown(Map<String, Object> event) {
        if(dbContainer) {
            dbContainer.start()
        }
    }
}