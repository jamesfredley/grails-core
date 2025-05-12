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
package grails.init;

import grails.proxy.SystemPropertiesAuthenticator;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.util.Arrays;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * The purpose of this class is to download the expanded Grails wrapper jars into GRAILS_HOME (`.grails` in the project root)
 * This class is not meant to be distributed as part of SDKMAN since we'll distribute the expanded jars with it.
 * After downloading the jars, it will delegate to the downloaded wrapper impl project
 */
public class Start {
    public static void main(String[] args) {
        Authenticator.setDefault(new SystemPropertiesAuthenticator());

        try {
            GrailsUpdater updater = new GrailsUpdater();
            boolean forceUpdate = (args.length > 0 && args[0].trim().equals("update-wrapper"));

            String[] adjustedArgs = args;
            if (forceUpdate || updater.needsUpdating()) {
                updater.update();

                // remove "update-wrapper" command argument
                if (forceUpdate) {
                    adjustedArgs = Arrays.copyOfRange(args, 1, args.length);
                }
            }

            URLClassLoader child = new URLClassLoader(new URL[]{updater.getExecutedVersion().toURI().toURL()});
            Class<?> classToLoad = Class.forName("grails.init.RunCommand", true, child);
            Method main = classToLoad.getMethod("main", String[].class);
            main.invoke(null, (Object[]) adjustedArgs);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
