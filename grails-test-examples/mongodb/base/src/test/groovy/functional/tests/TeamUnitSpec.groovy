package functional.tests

import grails.test.mongodb.MongoSpec
import org.apache.grails.testing.AbstractMongoGrailsExtension
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName
import spock.lang.AutoCleanup
import spock.lang.Shared

class TeamUnitSpec extends MongoSpec implements EmbeddedMongoClient {

    @Shared
    @AutoCleanup
    final MongoDBContainer mongoDBContainer = new MongoDBContainer(AbstractMongoGrailsExtension.desiredMongoDockerName)

    void "get() doesn't throw NPE"() {
        when:
        Team team = Team.get("123")

        then:
        !team
    }
}
