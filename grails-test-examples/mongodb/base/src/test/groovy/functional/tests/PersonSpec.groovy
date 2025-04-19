package functional.tests

import grails.test.mongodb.MongoSpec
import org.apache.grails.testing.AbstractMongoGrailsExtension
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName
import spock.lang.AutoCleanup
import spock.lang.Shared

/**
 * Created by graemerocher on 17/10/16.
 */
class PersonSpec extends MongoSpec implements EmbeddedMongoClient {

    @Shared
    @AutoCleanup
    final MongoDBContainer mongoDBContainer = new MongoDBContainer(AbstractMongoGrailsExtension.desiredMongoDockerName)

    void "Test codecs work for custom properties"() {
        when:"An an instance with a custom codec is saved"
        def dob = new Date()
        new Person(name: "Fred", birthday: new Birthday(dob)).save(flush:true)
        Person person = Person.first()

        then:"The result is correct"
        person.name == "Fred"
        person.birthday.date == dob
    }
}
