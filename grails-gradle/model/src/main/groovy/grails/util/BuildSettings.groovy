/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package grails.util

import grails.io.IOUtils
import groovy.transform.CompileStatic

/**
 * Build time settings and configuration
 *
 * @author Graeme Rocher
 */
@CompileStatic
class BuildSettings {

    /**
     * The http proxy username
     */
    public static final String PROXY_HTTP_USER = "http.proxyUser"
    /**
     * The http proxy password
     */
    public static final String PROXY_HTTP_PASSWORD = "http.proxyPassword"
    /**
     * The proxy selector object to use when connecting remotely from the CLI
     */
    public static final String PROXY_SELECTOR = "grails.proxy.selector"
    /**
     * The authenticator to use when connecting remotely from the CLI
     */
    public static final String AUTHENTICATOR = "grails.proxy.authenticator"
    /**
     * Name of the System property that specifies the main class name
     */
    public static final String MAIN_CLASS_NAME = "org.grails.MAIN_CLASS_NAME"

    /**
     * The name of the profile being used
     */
    public static final String PROFILE = "grails.profile"
    /**
     * Specifies the profile repositories to use
     */
    public static final String PROFILE_REPOSITORIES = "grails.profiles.repositories"

    /**
     * The base directory of the application
     */
    public static final String APP_BASE_DIR = "base.dir"

    /**
     * The name of the system property for {@link #}.
     */
    public static final String PROJECT_RESOURCES_DIR = "grails.project.resource.dir"

    /**
     * The name of the system property for the project classes directory. Must be set if changed from build/main/classes.
     */
    public static final String PROJECT_CLASSES_DIR = "grails.project.class.dir"

    /**
     *  A property name to enable/disable AST conversion of closures actions&tags to methods
     */
    public static final String CONVERT_CLOSURES_KEY = "grails.compile.artefacts.closures.convert"

    /**
     * The base directory of the project
     */
    public static final File BASE_DIR

    /**
     * Whether the application is running inside the development environment or deployed
     */
    public static final boolean GRAILS_APP_DIR_PRESENT

    /**
     * The target directory of the project, null outside of the development environment
     */
    public static final File TARGET_DIR
    /**
     * The resources directory of the project, null outside of the development environment
     */
    public static final File RESOURCES_DIR
    /**
     * The classes directory of the project, null outside of the development environment
     */
    public static final File CLASSES_DIR

    /**
     * The path to the build classes directory
     */
    public static final String BUILD_CLASSES_PATH

    /**
     * The path to the build resources directory
     */
    public static final String BUILD_RESOURCES_PATH = "build/resources/main"

    public static final File SHARED_SETTINGS_FILE = new File("${System.getProperty('user.home')}/.grails/settings.groovy")

    /**
     * @return The version of Grails being used
     */
    static String getGrailsVersion() {
        BuildSettings.package.implementationVersion
    }

    static {
        boolean grailsAppDirPresent = new File( "grails-app").exists() || new File( "Application.groovy").exists()
        if(!grailsAppDirPresent) {
            CLASSES_DIR = null
            BUILD_CLASSES_PATH = "build/classes/main"
        }
        else {
            String fromSystem = System.getProperty(PROJECT_CLASSES_DIR)
            if(fromSystem) {
                CLASSES_DIR = new File(fromSystem)
                BUILD_CLASSES_PATH = fromSystem
            }
            else  {
                File groovyDir = new File("build/classes/groovy/main")
                if(groovyDir.exists()) {
                    BUILD_CLASSES_PATH = "build/classes/groovy/main"
                    CLASSES_DIR = groovyDir
                }
                else {
                    BUILD_CLASSES_PATH = "build/classes/main"
                    CLASSES_DIR = new File("build/classes/main")
                }
            }
        }
        BASE_DIR = System.getProperty(APP_BASE_DIR) ? new File(System.getProperty(APP_BASE_DIR)) :  ( IOUtils.findApplicationDirectoryFile() ?: new File("."))
        GRAILS_APP_DIR_PRESENT = new File(BASE_DIR, "grails-app").exists() || new File(BASE_DIR, "Application.groovy").exists()
        TARGET_DIR = new File(BASE_DIR, "build")
        RESOURCES_DIR = !GRAILS_APP_DIR_PRESENT ? null : (System.getProperty(PROJECT_RESOURCES_DIR) ? new File(System.getProperty(PROJECT_RESOURCES_DIR)) : new File(TARGET_DIR, "resources/main"))
    }
}
