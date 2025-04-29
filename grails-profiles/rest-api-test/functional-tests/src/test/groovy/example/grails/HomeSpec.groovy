package example.grails

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import spock.lang.Specification

class HomeSpec extends Specification {

    def "root url returns 200"() {
        given:
        HttpClient client = HttpClient.create(new URL('http://localhost:8080'))

        when:
        HttpResponse response = client.toBlocking().exchange(HttpRequest.GET('/'))

        then:
        response.status() == HttpStatus.OK
    }
}
