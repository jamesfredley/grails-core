package org.demo.spock

import grails.plugin.geb.ContainerGebConfiguration
import grails.plugin.geb.ContainerGebSpec
import grails.testing.mixin.integration.Integration

/**
 * See https://docs.grails.org/latest/guide/testing.html#functionalTesting and https://www.gebish.org/manual/current/
 * for more instructions on how to write functional tests with Grails and Geb.
 */
@Integration
@ContainerGebConfiguration(hostName = 'testing.example.com')
class ServerNameControllerSpec extends ContainerGebSpec {

    void 'should show the right server name when visiting /serverName'() {
        when: 'visiting the server name controller'
        go '/serverName'

        then: 'the emitted hostname is correct'
        $('p').text() == 'Server name: testing.example.com'
    }
}
