package example.grails

import geb.Browser
import geb.spock.GebSpec
import spock.lang.Requires

class HomeSpec extends GebSpec {

    def "homepage title contains Grails"() {
        given:
        Browser browser = new Browser()

        when:
        browser.to(HomePage)

        then:
        at HomePage
    }
}
