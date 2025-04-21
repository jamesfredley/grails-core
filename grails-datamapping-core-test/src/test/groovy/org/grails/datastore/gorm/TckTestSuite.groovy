package org.grails.datastore.gorm

import grails.gorm.tck.NotInListSpec
import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite
import spock.lang.IgnoreIf

/**
 * Use this class to run tck classes against the current implementation.
 *
 * @author graemerocher
 */
@IgnoreIf({ os.windows }) // Fails on windows even though the NotInListSpec is in the TCK and runs
@Suite
@SelectClasses([NotInListSpec])
class TckTestSuite {
}
