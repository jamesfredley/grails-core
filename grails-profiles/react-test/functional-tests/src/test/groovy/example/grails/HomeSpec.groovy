package example.grails

import geb.Browser
import geb.spock.GebSpec

class HomeSpec extends GebSpec {

    def "homage shows a list of available controllers"() {
        given:
        Browser browser = new Browser()
        List<String> expectedControllerNames = ['demo.ApplicationController']

        when:
        HomePage homePage = browser.to(HomePage)
        List<String> controllerNames = homePage.controllerNames()

        then:
        expectedControllerNames == controllerNames
    }
}
