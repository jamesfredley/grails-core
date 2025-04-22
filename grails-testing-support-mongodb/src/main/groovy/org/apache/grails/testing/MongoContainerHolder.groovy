package org.apache.grails.testing

import groovy.transform.PackageScope
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.utility.DockerImageName

class MongoContainerHolder {
    private ThreadLocal<GenericContainer> containers = new ThreadLocal<GenericContainer>()
    final DockerImageName desiredImage

    MongoContainerHolder(DockerImageName desiredImage) {
        this.desiredImage = desiredImage
    }

    @PackageScope
    static GenericContainer startMongoContainer(DockerImageName dockerImageName) {
        GenericContainer dbContainer = new MongoDBContainer(dockerImageName)
        dbContainer.start()
        dbContainer.followOutput(new Slf4jLogConsumer(LoggerFactory.getLogger("testcontainers")))
        dbContainer
    }

    GenericContainer getContainer() {
        GenericContainer foundContainer = containers.get()
        if(foundContainer) {
            return foundContainer
        }

        GenericContainer startedContainer = startMongoContainer(desiredImage)
        containers.set(startedContainer)
        startedContainer
    }

    void stop() {
        containers.get()?.stop()
        containers.remove()
    }
}