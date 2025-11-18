package org.grails.web.converters.marshaller.json

import spock.lang.Specification

import org.springframework.context.ApplicationContext

import grails.converters.JSON
import grails.core.DefaultGrailsApplication
import grails.validation.Constrained
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.model.MappingContext
import org.grails.web.converters.configuration.ConvertersConfigurationInitializer

class StaticPropertySpec extends Specification {
    void initJson() {
        final initializer = new ConvertersConfigurationInitializer()
        def grailsApplication = new DefaultGrailsApplication(MyGroovyBean)
        grailsApplication.initialise()
        def mappingContext = new KeyValueMappingContext("json")
        grailsApplication.setApplicationContext(Stub(ApplicationContext) {
            getBean('grailsDomainClassMappingContext', MappingContext) >> {
                mappingContext
            }
        })
        grailsApplication.setMappingContext(mappingContext)
        initializer.grailsApplication = grailsApplication
        initializer.initialize()

    }

    void "static property should be excluded"() {
        given:
        initJson()

        when:
        MyGroovyBean bean = new MyGroovyBean(aProperty: 'testing')

        then:
        def jsonString = new JSON(bean).toString()
        jsonString == '{"aProperty":"testing"}'
    }
}

class MyGroovyBean {
    static Map<String, Constrained> getConstraintsMap() {
        [:]
    }

    String aProperty
}
