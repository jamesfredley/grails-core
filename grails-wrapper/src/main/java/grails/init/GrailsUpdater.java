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

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class GrailsUpdater {
    private static final String WRAPPER_MAVEN_PATH = "/org/apache/grails/" + GrailsHome.CLI_COMBINED_PROJECT_NAME;
    private static final String GRAILS_RELEASE_MAVEN_REPO_BASE_URL = "https://repository.apache.org/content/groups/public";

    private final GrailsHome grailsHome;
    private GrailsVersion updatedVersion;

    public GrailsUpdater() throws IOException {
        this(null);
    }

    public GrailsUpdater(String possibleGrailsHome) throws IOException {
        grailsHome = new GrailsHome(possibleGrailsHome);
    }

    public File getExecutedVersion() {
        return updatedVersion == null ? grailsHome.getLatestWrapperImplementation() : grailsHome.getWrapperImplementation(grailsHome.getVersionDirectory(updatedVersion));
    }

    /**
     * Reasons the grails wrapper may need updating:
     * 1. the expanded wrapper jar does not exist
     *
     */
    public boolean needsUpdating() {
        File jarFile = grailsHome.getLatestWrapperImplementation();
        if(jarFile == null) {
            return true;
        }

        // TODO: Should we force updates for snapshots?

        return false;
    }

    public boolean update() {
        GrailsVersion baseVersion = null;
        try {
            baseVersion = getVersion();
        }
        catch(Exception e) {
            System.err.println("You must be connected to the internet the first time you use the Grails wrapper");
            e.printStackTrace();
            System.exit(1);
        }

        String detailedVersion = null;
        if (baseVersion.releaseType.isSnapshot()) {
            try {
                detailedVersion = getSnapshotVersion(baseVersion);
            }
            catch(Exception e) {
                System.err.println("Could not parse snapshot version.  You must be connected to the internet the first time you use the Grails wrapper");
                e.printStackTrace();
                System.exit(1);
            }
        }

        boolean theResult = updateJar(baseVersion, detailedVersion);
        if(theResult) {
            updatedVersion = baseVersion;
        }

        return theResult;
    }

    private boolean updateJar(GrailsVersion version, String snapshotVersion) {
        boolean success = false;

        final String localJarFilename = GrailsHome.CLI_COMBINED_PROJECT_NAME + "-" + version.version;
        final String remoteJarFilename = snapshotVersion != null ? GrailsHome.CLI_COMBINED_PROJECT_NAME + "-" + snapshotVersion : GrailsHome.CLI_COMBINED_PROJECT_NAME + "-" + version.version;
        final String jarFileExtension = ".jar";

        try {
            File downloadedJar = File.createTempFile(localJarFilename, jarFileExtension);
            String wrapperUrl = getMavenBaseUrl() + WRAPPER_MAVEN_PATH + "/" + version.version + "/" + remoteJarFilename + jarFileExtension;
            HttpURLConnection conn = createHttpURLConnection(wrapperUrl);
            success = downloadWrapperJar(version, downloadedJar, conn.getInputStream());
        } catch (Exception e) {
            System.err.println("There was an error downloading the wrapper jar");
            e.printStackTrace();
        }
        return success;
    }

    private boolean downloadWrapperJar(GrailsVersion toKeep, File downloadJarLocation, InputStream inputStream) throws IOException {
        ReadableByteChannel rbc = Channels.newChannel(inputStream);
        try (FileOutputStream fos = new FileOutputStream(downloadJarLocation)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }

        try {
            grailsHome.cleanupOtherVersions(toKeep);
        }
        catch(Exception e) {
            System.err.println("Unable to cleanup old versions of the wrapper");
            e.printStackTrace();
        }

        File directory = grailsHome.getVersionDirectory(toKeep);
        Files.move(downloadJarLocation.getAbsoluteFile().toPath(), new File(directory, downloadJarLocation.getName()).getAbsoluteFile().toPath(), REPLACE_EXISTING);

        return true;
    }

    private static String getMavenBaseUrl() {
        String baseUrl = System.getProperty("grails.maven.repo.baseUrl");
        if (baseUrl != null) {
            return baseUrl;
        }

        baseUrl = System.getenv("GRAILS_RELEASE_MAVEN_REPO_BASE_URL");
        if (baseUrl != null) {
            return baseUrl;
        }

        return GRAILS_RELEASE_MAVEN_REPO_BASE_URL;
    }

    private static GrailsVersion getVersion() throws IOException, SAXException, ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        FindReleaseHandler findReleaseHandler = new FindReleaseHandler();
        final String mavenMetadataFileUrl = getMavenBaseUrl() + WRAPPER_MAVEN_PATH + "/maven-metadata.xml";
        HttpURLConnection conn = createHttpURLConnection(mavenMetadataFileUrl);

        try(InputStream stream = conn.getInputStream()) {
            saxParser.parse(stream, findReleaseHandler);
            String parsedVersion = findReleaseHandler.getVersion();
            try {
                return new GrailsVersion(parsedVersion);
            }
            catch(Exception e) {
                throw new IllegalStateException("Failed to parse version '" + parsedVersion + "' from maven repository.", e);
            }
        }
    }

    private static String getSnapshotVersion(GrailsVersion baseVersion) throws IOException, SAXException, ParserConfigurationException {
        System.out.println("A Grails snapshot version has been detected.  Downloading latest snapshot.");
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        FindSnapshotHandler findVersionHandler = new FindSnapshotHandler();
        final String mavenMetadataFileUrl = getMavenBaseUrl() + WRAPPER_MAVEN_PATH + "/" + baseVersion.version + "/maven-metadata.xml";
        HttpURLConnection conn = createHttpURLConnection(mavenMetadataFileUrl);
        try(InputStream stream = conn.getInputStream()) {
            saxParser.parse(stream, findVersionHandler);
            return findVersionHandler.getVersion();
        }
    }

    private static HttpURLConnection createHttpURLConnection(String mavenMetadataFileUrl) throws IOException {
        final URL url = new URL(mavenMetadataFileUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "Apache-Maven/3.9.6");
        conn.setInstanceFollowRedirects(true);
        return conn;
    }
}
