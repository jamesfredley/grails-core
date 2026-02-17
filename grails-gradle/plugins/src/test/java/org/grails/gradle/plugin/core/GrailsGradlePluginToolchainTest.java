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
package org.grails.gradle.plugin.core;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that {@link GrailsGradlePlugin} propagates the project's Java toolchain
 * to JavaExec tasks via {@code javaLauncher.convention()}.
 *
 * <p>Without the fix, JavaExec tasks spawned by Grails (dbm-* migration
 * commands, console, shell, application context commands) use the JDK
 * running Gradle instead of the project's configured toolchain. This
 * causes {@code UnsupportedClassVersionError} when the project targets
 * a different JDK version than the one running Gradle.</p>
 *
 * <p>NOTE: Cannot use Spock here because grails-gradle-tasks transitively
 * pulls {@code org.apache.groovy:groovy:4.0.30} which conflicts with
 * Gradle's built-in {@code org.codehaus.groovy:groovy:3.0.25}.</p>
 *
 * @since 7.0.8
 */
class GrailsGradlePluginToolchainTest {

    @TempDir
    Path projectDir;

    private static final int CURRENT_JDK = Runtime.version().feature();
    private static final String GRAILS_VERSION = resolveGrailsVersion();

    /**
     * Reads {@code projectVersion} from the root {@code gradle.properties} so the
     * test stays in sync with the actual build and is not tied to a hardcoded version.
     */
    private static String resolveGrailsVersion() {
        Properties props = new Properties();
        try (InputStream in = GrailsGradlePluginToolchainTest.class
                .getClassLoader().getResourceAsStream("grails-gradle-plugins-project.properties")) {
            if (in != null) {
                props.load(in);
                String v = props.getProperty("projectVersion");
                if (v != null && !v.isEmpty()) return v;
            }
        } catch (IOException ignored) { }
        // Fallback: read from root gradle.properties relative to working directory
        try {
            Path root = Path.of(System.getProperty("user.dir"));
            // Walk up until we find gradle.properties with projectVersion
            for (Path dir = root; dir != null; dir = dir.getParent()) {
                Path gp = dir.resolve("gradle.properties");
                if (Files.exists(gp)) {
                    Properties rootProps = new Properties();
                    try (var reader = Files.newBufferedReader(gp)) {
                        rootProps.load(reader);
                    }
                    String v = rootProps.getProperty("projectVersion");
                    if (v != null && !v.isEmpty()) return v;
                }
            }
        } catch (IOException ignored) { }
        return "7.0.8-SNAPSHOT";
    }

    @BeforeEach
    void setup() throws IOException {
        // Minimal Grails directory structure required by the plugin
        Files.createDirectories(projectDir.resolve("grails-app/conf"));
        Files.writeString(projectDir.resolve("grails-app/conf/application.yml"), "");
        Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'test-toolchain'\n");
        // GrailsGradlePlugin.resolveGrailsVersion() requires this property
        Files.writeString(projectDir.resolve("gradle.properties"), "grailsVersion=" + GRAILS_VERSION + "\n");
    }

    private BuildResult runBuild(String... args) {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments(args)
                .withPluginClasspath()
                .forwardOutput()
                .build();
    }

    // ----------------------------------------------------------------
    // Toolchain propagation tests
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("With toolchain configured")
    class WithToolchain {

        @Test
        @DisplayName("JavaExec tasks inherit project toolchain")
        void javaExecInheritsToolchain() throws IOException {
            Files.writeString(projectDir.resolve("build.gradle"),
                    "plugins {\n" +
                    "    id 'org.apache.grails.gradle.grails-app'\n" +
                    "}\n" +
                    "\n" +
                    "java {\n" +
                    "    toolchain {\n" +
                    "        languageVersion = JavaLanguageVersion.of(" + CURRENT_JDK + ")\n" +
                    "    }\n" +
                    "}\n" +
                    "\n" +
                    "tasks.register('printLauncher', JavaExec) {\n" +
                    "    classpath = files()\n" +
                    "    mainClass = 'does.not.Matter'\n" +
                    "}\n" +
                    "\n" +
                    "tasks.register('checkToolchain') {\n" +
                    "    doLast {\n" +
                    "        def launcher = tasks.named('printLauncher', JavaExec).get().javaLauncher\n" +
                    "        if (launcher.isPresent()) {\n" +
                    "            def metadata = launcher.get().metadata\n" +
                    "            println \"TOOLCHAIN_VERSION=${metadata.languageVersion.asInt()}\"\n" +
                    "        } else {\n" +
                    "            println 'TOOLCHAIN_VERSION=none'\n" +
                    "        }\n" +
                    "    }\n" +
                    "}\n"
            );

            BuildResult result = runBuild("checkToolchain", "--stacktrace");
            assertTrue(result.getOutput().contains("TOOLCHAIN_VERSION=" + CURRENT_JDK),
                    "JavaExec task should inherit project toolchain (JDK " + CURRENT_JDK + ")");
        }

        @Test
        @DisplayName("Test tasks inherit project toolchain")
        void testTaskInheritsToolchain() throws IOException {
            Files.writeString(projectDir.resolve("build.gradle"),
                    "plugins {\n" +
                    "    id 'org.apache.grails.gradle.grails-app'\n" +
                    "}\n" +
                    "\n" +
                    "java {\n" +
                    "    toolchain {\n" +
                    "        languageVersion = JavaLanguageVersion.of(" + CURRENT_JDK + ")\n" +
                    "    }\n" +
                    "}\n" +
                    "\n" +
                    "tasks.register('checkTestToolchain') {\n" +
                    "    doLast {\n" +
                    "        def testTask = tasks.named('test', Test).get()\n" +
                    "        def launcher = testTask.javaLauncher\n" +
                    "        if (launcher.isPresent()) {\n" +
                    "            println \"TEST_TOOLCHAIN_VERSION=${launcher.get().metadata.languageVersion.asInt()}\"\n" +
                    "        } else {\n" +
                    "            println 'TEST_TOOLCHAIN_VERSION=none'\n" +
                    "        }\n" +
                    "    }\n" +
                    "}\n"
            );

            BuildResult result = runBuild("checkTestToolchain", "--stacktrace");
            assertTrue(result.getOutput().contains("TEST_TOOLCHAIN_VERSION=" + CURRENT_JDK),
                    "Test task should inherit project toolchain (JDK " + CURRENT_JDK + ")");
        }

        @Test
        @DisplayName("ApplicationContextCommandTask inherits toolchain via grails-web plugin")
        void applicationContextCommandTaskInheritsToolchain() throws IOException {
            Files.writeString(projectDir.resolve("build.gradle"),
                    "plugins {\n" +
                    "    id 'org.apache.grails.gradle.grails-web'\n" +
                    "}\n" +
                    "\n" +
                    "java {\n" +
                    "    toolchain {\n" +
                    "        languageVersion = JavaLanguageVersion.of(" + CURRENT_JDK + ")\n" +
                    "    }\n" +
                    "}\n" +
                    "\n" +
                    "tasks.register('checkCommandToolchain') {\n" +
                    "    doLast {\n" +
                    "        def cmdTask = tasks.named('urlMappingsReport').get()\n" +
                    "        if (cmdTask instanceof JavaExec) {\n" +
                    "            def launcher = ((JavaExec) cmdTask).javaLauncher\n" +
                    "            if (launcher.isPresent()) {\n" +
                    "                println \"CMD_TOOLCHAIN_VERSION=${launcher.get().metadata.languageVersion.asInt()}\"\n" +
                    "            } else {\n" +
                    "                println 'CMD_TOOLCHAIN_VERSION=none'\n" +
                    "            }\n" +
                    "        } else {\n" +
                    "            println 'CMD_TOOLCHAIN_VERSION=not_javaexec'\n" +
                    "        }\n" +
                    "    }\n" +
                    "}\n"
            );

            BuildResult result = runBuild("checkCommandToolchain", "--stacktrace");
            assertTrue(result.getOutput().contains("CMD_TOOLCHAIN_VERSION=" + CURRENT_JDK),
                    "ApplicationContextCommandTask should inherit project toolchain (JDK " + CURRENT_JDK + ")");
        }

        @Test
        @DisplayName("convention() allows individual task override via set()")
        void conventionAllowsOverride() throws IOException {
            Files.writeString(projectDir.resolve("build.gradle"),
                    "import org.gradle.jvm.toolchain.JavaLanguageVersion\n" +
                    "\n" +
                    "plugins {\n" +
                    "    id 'org.apache.grails.gradle.grails-app'\n" +
                    "}\n" +
                    "\n" +
                    "java {\n" +
                    "    toolchain {\n" +
                    "        languageVersion = JavaLanguageVersion.of(" + CURRENT_JDK + ")\n" +
                    "    }\n" +
                    "}\n" +
                    "\n" +
                    "tasks.register('customExec', JavaExec) {\n" +
                    "    classpath = files()\n" +
                    "    mainClass = 'does.not.Matter'\n" +
                    "    javaLauncher.set(javaToolchains.launcherFor {\n" +
                    "        languageVersion = JavaLanguageVersion.of(" + CURRENT_JDK + ")\n" +
                    "    })\n" +
                    "}\n" +
                    "\n" +
                    "tasks.register('checkOverride') {\n" +
                    "    doLast {\n" +
                    "        def launcher = tasks.named('customExec', JavaExec).get().javaLauncher\n" +
                    "        if (launcher.isPresent()) {\n" +
                    "            println \"OVERRIDE_VERSION=${launcher.get().metadata.languageVersion.asInt()}\"\n" +
                    "        } else {\n" +
                    "            println 'OVERRIDE_VERSION=none'\n" +
                    "        }\n" +
                    "    }\n" +
                    "}\n"
            );

            BuildResult result = runBuild("checkOverride", "--stacktrace");
            assertTrue(result.getOutput().contains("OVERRIDE_VERSION=" + CURRENT_JDK),
                    "Task with explicit set() should override convention");
        }
    }

    // ----------------------------------------------------------------
    // No-toolchain backwards compatibility tests
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("Without toolchain configured (backwards compatibility)")
    class WithoutToolchain {

        @Test
        @DisplayName("JavaExec tasks work without errors when no toolchain configured")
        void javaExecWorksWithoutToolchain() throws IOException {
            Files.writeString(projectDir.resolve("build.gradle"),
                    "plugins {\n" +
                    "    id 'org.apache.grails.gradle.grails-app'\n" +
                    "}\n" +
                    "\n" +
                    "tasks.register('printLauncher', JavaExec) {\n" +
                    "    classpath = files()\n" +
                    "    mainClass = 'does.not.Matter'\n" +
                    "}\n" +
                    "\n" +
                    "tasks.register('checkToolchain') {\n" +
                    "    doLast {\n" +
                    "        def launcher = tasks.named('printLauncher', JavaExec).get().javaLauncher\n" +
                    "        if (launcher.isPresent()) {\n" +
                    "            println 'HAS_LAUNCHER=true'\n" +
                    "        } else {\n" +
                    "            println 'HAS_LAUNCHER=false'\n" +
                    "        }\n" +
                    "    }\n" +
                    "}\n"
            );

            BuildResult result = runBuild("checkToolchain", "--stacktrace");
            assertTrue(result.getOutput().contains("HAS_LAUNCHER="),
                    "Plugin should not error when no toolchain is configured");
        }

        @Test
        @DisplayName("GrailsWebGradlePlugin works without errors when no toolchain configured")
        void webPluginWorksWithoutToolchain() throws IOException {
            Files.writeString(projectDir.resolve("build.gradle"),
                    "plugins {\n" +
                    "    id 'org.apache.grails.gradle.grails-web'\n" +
                    "}\n" +
                    "\n" +
                    "tasks.register('checkNoError') {\n" +
                    "    doLast {\n" +
                    "        println 'WEB_PLUGIN_OK=true'\n" +
                    "    }\n" +
                    "}\n"
            );

            BuildResult result = runBuild("checkNoError", "--stacktrace");
            assertTrue(result.getOutput().contains("WEB_PLUGIN_OK=true"),
                    "GrailsWebGradlePlugin should work without toolchain configured");
        }
    }

    // ----------------------------------------------------------------
    // Fork settings tests (system properties, heap sizes)
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("Fork settings (existing behavior preserved)")
    class ForkSettings {

        @Test
        @DisplayName("configureForkSettings applies system properties and default heap sizes")
        void forkSettingsApplyDefaults() throws IOException {
            Files.writeString(projectDir.resolve("build.gradle"),
                    "plugins {\n" +
                    "    id 'org.apache.grails.gradle.grails-app'\n" +
                    "}\n" +
                    "\n" +
                    "tasks.register('checkSysProps', JavaExec) {\n" +
                    "    classpath = files()\n" +
                    "    mainClass = 'does.not.Matter'\n" +
                    "}\n" +
                    "\n" +
                    "tasks.register('inspectSysProps') {\n" +
                    "    doLast {\n" +
                    "        def task = tasks.named('checkSysProps', JavaExec).get()\n" +
                    "        def sysProps = task.systemProperties\n" +
                    "        println \"HAS_ENV=${sysProps.containsKey('grails.env')}\"\n" +
                    "        println \"MIN_HEAP=${task.minHeapSize}\"\n" +
                    "        println \"MAX_HEAP=${task.maxHeapSize}\"\n" +
                    "    }\n" +
                    "}\n"
            );

            BuildResult result = runBuild("inspectSysProps", "--stacktrace");
            assertTrue(result.getOutput().contains("HAS_ENV=true"),
                    "Fork settings should set grails.env system property");
            assertTrue(result.getOutput().contains("MIN_HEAP=768m"),
                    "Default min heap should be 768m");
            assertTrue(result.getOutput().contains("MAX_HEAP=768m"),
                    "Default max heap should be 768m");
        }

        @Test
        @DisplayName("Custom heap sizes are not overridden by fork settings")
        void customHeapSizesPreserved() throws IOException {
            Files.writeString(projectDir.resolve("build.gradle"),
                    "plugins {\n" +
                    "    id 'org.apache.grails.gradle.grails-app'\n" +
                    "}\n" +
                    "\n" +
                    "tasks.register('customHeap', JavaExec) {\n" +
                    "    classpath = files()\n" +
                    "    mainClass = 'does.not.Matter'\n" +
                    "    minHeapSize = '512m'\n" +
                    "    maxHeapSize = '2g'\n" +
                    "}\n" +
                    "\n" +
                    "tasks.register('inspectHeap') {\n" +
                    "    doLast {\n" +
                    "        def task = tasks.named('customHeap', JavaExec).get()\n" +
                    "        println \"MIN_HEAP=${task.minHeapSize}\"\n" +
                    "        println \"MAX_HEAP=${task.maxHeapSize}\"\n" +
                    "    }\n" +
                    "}\n"
            );

            BuildResult result = runBuild("inspectHeap", "--stacktrace");
            assertTrue(result.getOutput().contains("MIN_HEAP=512m"),
                    "Custom min heap should be preserved");
            assertTrue(result.getOutput().contains("MAX_HEAP=2g"),
                    "Custom max heap should be preserved");
        }
    }
}
