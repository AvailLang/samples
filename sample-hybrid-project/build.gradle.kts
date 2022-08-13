/*
 * build.gradle.kts
 * Copyright © 1993-2022, The Avail Foundation, LLC.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of the contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
import org.availlang.artifact.AvailArtifactType.APPLICATION
import org.availlang.artifact.PackageType.JAR
import org.availlang.artifact.jar.JvmComponent
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
    kotlin("jvm") version Versions.kotlin
    id("avail.avail-plugin") version Versions.availGradle
}

group = "org.availlang.sample"
version = "1.0"

repositories {
    mavenLocal {
        url = uri("local-plugin-repository/")
    }
    mavenLocal()
    mavenCentral()
}

val jvmTarget = 17
val jvmTargetString = "17"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(Versions.jvmTarget))
    }
}

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(
            JavaLanguageVersion.of(Versions.jvmTarget))
    }
}

dependencies {
    // The module that is the foreign function interface that provides Pojos
    // written in Java that is usable by Avail.
    implementation(project(":avail-java-ffi"))
    implementation("org.availlang:avail-json:1.1.1")

    // Dependency prevents SLF4J warning from being printed
    // see: http://www.slf4j.org/codes.html#noProviders
//    implementation("org.slf4j:slf4j-nop:${Versions.slf4jnop}")

    // Can add an Avail library dependency as a jar available in one of the
    // repositories listed in the repository section
    // availLibrary("avail:example-lib:1.2.3")
    testImplementation(kotlin("test"))
    implementation("org.availlang:avail:1.6.2-SNAPSHOT")
}

// This block configures an AvailExtension instance that is used by the Avail
// Gradle plugin for configuring the Avail application.
avail {
    projectDescription = "This description goes into the Avail manifest in the jar!"

    // This imports the Avail Standard Library from a Maven repository,
    // presumably Maven Central where the library is officially published.
    includeStdAvailLibDependency {
        // The name of the root for the standard library actually defaults to
        // "avail", so it is not necessary to include this line.
        name = "avail"

        // The dependency artifact group. This defaults to "org.availlang", so
        // it is not necessary to include this line.
        group = "org.availlang"

        // The dependency artifact name. This defaults to "avail-stdlib", so it
        // is not necessary to include this line.
        artifactName = "avail-stdlib"

        // OPTIONAL: The specific dependency version of the published  Avail
        // Standard Library. If not explicitly set, the most recently released
        // version of the standard library will be used. The most recent version
        // being used is indicated by a version set to `+`.
        version = "2.0.0-1.6.1-SNAPSHOT"
    }

    // Adds an Avail library from a dependency from
    // includeAvailLibDependency("sample", "org.mystuff:alib:1.0.0")

    // Specify where the main Avail roots' directory is located.
    rootsDirectory = "$projectDir/avail/my-roots"
    // Specify where to write the .repo files to.
    repositoryDirectory = "$projectDir/avail/my-repos"

    // Point to a file that contains the file header comment body to be used
    // by all generated modules.
    moduleHeaderCommentBodyFile = "$projectDir/copyright.txt"

    // Add this new root to the roots directory and create it. Will only create
    // files in this root that do not already exist.
    createRoot("my-avail-root").apply{
        val customHeader =
            "Copyright © 1993-2022, The Avail Foundation, LLC.\n" +
                "All rights reserved."
        // Add a module package to this created root. Only happens if file does
        // not exist.
        modulePackage("App").apply{
            // Specify module header for package representative.
            versions = listOf("Avail-1.6.0")
            // The modules to extend in the Avail header.
            extends = listOf("Avail", "Configurations", "Network")
            // Add a module to this module package.
            addModule("Configurations").apply {
                // Specify module header for this module..
                versions = listOf("Avail-1.6.0")
                // The modules to list in the uses section in the Avail header.
                uses = listOf("Avail")
                // Override the module header comment from
                // moduleHeaderCommentBodyFile
                moduleHeaderCommentBody = customHeader
            }
            // Add a module package to this module package.
            addModulePackage("Network").apply {
                println("Setting up Network.avail")
                versions = listOf("Avail-1.6.0")
                uses = listOf("Avail")
                extends = listOf("Server")
                moduleHeaderCommentBody = customHeader
                addModule("Server").apply {
                    versions = listOf("Avail-1.6.0")
                    uses = listOf("Avail")
                    moduleHeaderCommentBody = customHeader
                }
            }
        }

        // Add a module to the top level of the created root.
        module("Scripts").apply {
            versions = listOf("Avail-1.6.0")
            uses = listOf("Avail")
            moduleHeaderCommentBody = customHeader
        }
    }

    // This represents a PackageAvailArtifact. It is used to configure the
    // creation of an Avail artifact.
    artifact {
        // The AvailArtifactType; either LIBRARY or APPLICATION. The default
        // is APPLICATION.
        artifactType = APPLICATION

        // The PackageType that indicates how the Avail artifact is to be
        // packaged. Packaging as a JAR is the default setting. At time of
        // writing on JAR files were supported for packaging.
        packageType = JAR

        // The version that is set for the artifact. This is set to the
        // project's version by default.
        version = project.version.toString()

        // The [Attributes.Name.IMPLEMENTATION_TITLE inside the JAR file
        // MANIFEST.MF.
        implementationTitle = "Avail Sample Hybrid Application"

        // The [Attributes.Name.MAIN_CLASS] for the manifest or an empty string
        // if no main class set. This should be the primary main class for
        // starting the application.
        jarManifestMainClass = "org.availlang.sample.AppKt"

        // The JvmComponent that describes the JVM contents of the artifact or
        // JvmComponent.NONE if no JVM components. JvmComponent.NONE is the
        // default.
        jvmComponent = JvmComponent(
            true,
            "It's got java bits!",
            mapOf("org.availlang.sample.AppKt" to "It runs the app!"))

        // The location to place the artifact. The value shown is the default
        // location.
        outputDirectory = "${project.buildDir}/libs/"
    }
}

// A helper getter to obtain the AvailExtension object configured in the
// `avail {}` block above.
val availExtension get() = project.extensions
    .findByType(avail.plugin.AvailExtension::class.java)!!

tasks {

    jar {
        doLast {
            availExtension.createArtifact()
        }
    }

    withType<KotlinCompile>() {
        kotlinOptions.jvmTarget = jvmTargetString
    }

    withType<JavaCompile>() {
        sourceCompatibility = jvmTargetString
        targetCompatibility = jvmTargetString
    }
    jar {
        archiveVersion.set("")
    }
    test {
        useJUnit()
        val toolChains =
            project.extensions.getByType(JavaToolchainService::class)
        javaLauncher.set(
            toolChains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(
                    Versions.jvmTarget))
            })
        testLogging {
            events = setOf(FAILED)
            exceptionFormat = TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
    }
}
