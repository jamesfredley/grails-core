/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package micronaut

import io.github.cjstehno.ersatz.ErsatzServer
import io.github.cjstehno.ersatz.cfg.ContentType
import io.micronaut.context.ApplicationContext as MicronautApplicationContext
import io.micronaut.http.client.HttpClient
import micronaut.client.MicronautTestClient
import spock.lang.AutoCleanup
import spock.lang.Specification

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

import grails.testing.mixin.integration.Integration

@Integration
class MicronautDeclarativeClientSpec extends Specification {

    @Autowired
    MicronautApplicationContext micronautContext

    @Value('${local.server.port}')
    Integer serverPort

    @AutoCleanup
    ErsatzServer ersatz = new ErsatzServer({ cfg ->
        cfg.httpPort(19876)
    })

    void "declarative @Client interface is registered as a bean in Micronaut context"() {
        when: 'looking up the declarative client bean'
        def client = micronautContext.getBean(MicronautTestClient)

        then: 'the bean is found and implements the interface'
        client != null
        client instanceof MicronautTestClient
    }

    void "declarative @Client invokes endpoint through load balancing path"() {
        given: 'an ersatz server mocking the expected endpoint response'
        ersatz.expectations({ expect ->
            expect.GET('/micronaut-test', { req ->
                req.called(1)
                req.responder({ res ->
                    res.code(200)
                    res.body(
                            '{"javaMessage":"hello","factoryName":"test-factory"}',
                            ContentType.APPLICATION_JSON
                    )
                })
            })
        })

        and: 'the declarative client from the Micronaut context'
        def client = micronautContext.getBean(MicronautTestClient)

        when: 'invoking the client method which goes through service discovery and load balancing'
        def response = client.index()

        then: 'the response contains the expected JSON from the ersatz mock'
        response != null
        response.contains('javaMessage')
        response.contains('factoryName')

        and: 'the ersatz server received exactly one request'
        ersatz.verify()
    }

    void "Micronaut HttpClient can reach the running Grails application"() {
        given: 'a Micronaut HTTP client targeting the running server'
        def client = HttpClient.create("http://localhost:$serverPort".toURL())

        when: 'calling the root endpoint'
        def response = client.toBlocking().exchange('/')

        then: 'the server responds successfully'
        response.status.code == 200

        cleanup:
        client.close()
    }
}
