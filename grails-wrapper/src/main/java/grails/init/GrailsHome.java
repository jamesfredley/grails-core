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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class GrailsHome {
    public static final String CLI_COMBINED_PROJECT_NAME = "grails-cli";
    public static final List<String> GRAILS_HOME_MARKERS = List.of("settings.gradle", "gradlew", "gradlew.bat");

    public final File home;
    public final File wrapperDirectory;
    public final List<GrailsVersion> versions;
    public final GrailsVersion latestVersion;

    public GrailsHome(String grailsHome) throws IOException {
        home = findGrailsHome(grailsHome).getCanonicalFile();

        wrapperDirectory = new File(home, "wrapper");
        if(!wrapperDirectory.exists()) {
            wrapperDirectory.mkdirs();
        }
        else if(!wrapperDirectory.isDirectory()) {
            throw new IllegalStateException("GRAILS_HOME must contain a wrapper directory. File exists instead at " + wrapperDirectory.getAbsolutePath());
        }

        versions = determineVersions();
        latestVersion = findLatestVersion();
    }

    File getLatestVersionDirectory() {
        return getVersionDirectory(latestVersion);
    }

    File getVersionDirectory(GrailsVersion version) {
        if(version == null) {
            return null;
        }

        return new File(wrapperDirectory, version.version);
    }

    File getLatestWrapperImplementation() {
        File implDirectory = getLatestVersionDirectory();
        return getWrapperImplementation(implDirectory);
    }

    File getWrapperImplementation(File implDirectory) {
        if(implDirectory == null) {
            return null;
        }

        return new File(implDirectory, CLI_COMBINED_PROJECT_NAME + ".jar");
    }

    void cleanupOtherVersions(GrailsVersion toKeep) {
        File[] children = wrapperDirectory.listFiles();
        if(children == null) {
            return;
        }

        for (File child : children) {
            if (!child.isDirectory()) {
                continue;
            }

            try {
                GrailsVersion version = new GrailsVersion(child.getName());
                if (version.equals(toKeep)) {
                    continue;
                }
            } catch (Exception ignored) {
                // Ignore invalid versions
            }

            child.delete();
        }
    }

    private GrailsVersion findLatestVersion() {
        if(versions.isEmpty()) {
            return null;
        }

        GrailsVersion lastRelease = null;
        GrailsVersion lastReleaseCandidate = null;
        GrailsVersion lastMilestone = null;
        GrailsVersion lastSnapshot = null;
        for (GrailsVersion version : versions) {
            if(version.releaseType == GrailsVersion.ReleaseType.RELEASE && (lastRelease == null || version.compareTo(lastRelease) > 0)) {
                lastRelease = version;
            }
            else if(version.releaseType == GrailsVersion.ReleaseType.RC && (lastReleaseCandidate == null || version.compareTo(lastReleaseCandidate) > 0)) {
                lastReleaseCandidate = version;
            }
            else if(version.releaseType == GrailsVersion.ReleaseType.MILESTONE && (lastMilestone == null || version.compareTo(lastMilestone) > 0)) {
                lastMilestone = version;
            }
            else if(version.releaseType == GrailsVersion.ReleaseType.SNAPSHOT && (lastSnapshot == null || version.compareTo(lastSnapshot) > 0)) {
                lastSnapshot = version;
            }
        }

        List<GrailsVersion> sortedVersions = Stream.of(
                        lastRelease,
                        lastReleaseCandidate,
                        lastMilestone,
                        lastSnapshot
                )
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        return sortedVersions.isEmpty() ? null : sortedVersions.get(sortedVersions.size() - 1);
    }

    /**
     * Finds the available versions in the GRAILS_HOME directory; sorted by smallest version to largest
     */
    private List<GrailsVersion> determineVersions() {
        File[] children = wrapperDirectory.listFiles();
        if(children == null || children.length == 0) {
            return new ArrayList<>();
        }

        List<GrailsVersion> versions = new ArrayList<>();
        for(File child : children) {
            if(!child.isDirectory()) {
                continue;
            }

            try {
                GrailsVersion version = new GrailsVersion(child.getName());
                versions.add(version);
            }
            catch(Exception ignored) {
                throw new IllegalStateException("Grails Version [" + child.getName() + "] at [" + child.getAbsolutePath() + "] is not a valid Grails version.");
            }
        }

        Collections.sort(versions);

        return versions;
    }

    /**
     * Locate the “Grails" home by:
     * 1. using the specified home
     * 2. using the environment variable
     * 3. Looking in the current directory for a GRAILS_HOME_MARKERS or for a .grails directory
     * and all parent directories.  If none, is found, the current directory will be returned.
     *
     * @return the GRAILS_HOME directory
     * @throws IOException if canonicalization fails
     */
    public static File findGrailsHome(String possibleGrailsHome) throws IOException {
        if (possibleGrailsHome != null && !possibleGrailsHome.isEmpty()) {
            return validateGrailsHome(possibleGrailsHome, "Specified Grails Home");
        }

        String environmentOverride = System.getenv("GRAILS_HOME");
        if (environmentOverride != null && !environmentOverride.isEmpty()) {
            return validateGrailsHome(environmentOverride, "GRAILS_HOME environment variable");
        }

        return locateGrailsHome(new File("."));
    }

    private static File validateGrailsHome(String possibleGrailsHome, String description) {
        File possibleHome = new File(possibleGrailsHome);
        if (!possibleHome.exists()) {
            possibleHome.mkdirs();
        }

        if (!possibleHome.isDirectory()) {
            throw new IllegalArgumentException(description + " [" + possibleGrailsHome + "] is not a directory");
        }

        return possibleHome;
    }

    private static boolean exists(File baseDirectory, String name) {
        File file = new File(baseDirectory, name);
        return file.exists();
    }

    /**
     * Locate the “Grails" home by first looking in `directory` for a GRAILS_HOME_MARKERS or for a .grails directory
     * and all parent directories.  If none, is found, the original directory will be returned.
     *
     * @param directory where to begin the search
     * @return the directory containing one of the markers, or original directory
     * @throws IOException if canonicalization fails
     */
    private static File locateGrailsHome(File directory) throws IOException {
        if (directory == null) {
            throw new IllegalArgumentException("Cannot search for GRAILS_HOME from a null directory.");
        }

        File userHome = new File(System.getProperty("user.home")).getCanonicalFile();

        File searchDirectory = directory.getCanonicalFile();
        if (searchDirectory.equals(userHome)) {
            throw new IllegalStateException("GRAILS_HOME is no longer allowed to be at the user home directory.");
        }

        if (searchDirectory.getParentFile() == null) {
            throw new IllegalStateException("GRAILS_HOME is not permitted to be at the root of the file system.");
        }

        File originalDirectory = searchDirectory;
        while (searchDirectory != null && searchDirectory.exists() && !searchDirectory.equals(userHome)) {
            for (String name : GRAILS_HOME_MARKERS) {
                if (exists(searchDirectory, name)) {
                    return searchDirectory;
                }
            }

            File grailsDir = new File(searchDirectory, ".grails");
            if (grailsDir.exists() && grailsDir.isDirectory()) {
                return searchDirectory;
            }

            searchDirectory = searchDirectory.getParentFile();
            if (searchDirectory != null) {
                searchDirectory = searchDirectory.getCanonicalFile();
            }
        }

        return originalDirectory;
    }
}

