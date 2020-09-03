/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import gradlebuild.basics.util.ReproduciblePropertiesWriter
import java.util.Properties

plugins {
    id("gradlebuild.internal.java")
}

dependencies {
    implementation("org.gradle:base-services")
    implementation("org.gradle:messaging")
    implementation("org.gradle:native")
    implementation("org.gradle:logging")
    implementation("org.gradle:cli")
    implementation("org.gradle:process-services")
    implementation("org.gradle:core-api")
    implementation("org.gradle:model-core")
    implementation("org.gradle:base-services-groovy")
    implementation("org.gradle:files")
    implementation("org.gradle:file-collections")
    implementation("org.gradle:resources")
    implementation("org.gradle:build-cache")
    implementation("org.gradle:persistent-cache")
    implementation("org.gradle:dependency-management")
    implementation("org.gradle:configuration-cache")
    implementation("org.gradle:jvm-services")
    implementation("org.gradle:launcher")
    implementation("org.gradle:internal-testing")
    implementation("org.gradle:build-events")
    implementation("org.gradle:build-option")

    implementation(libs.groovy)
    implementation(libs.junit)
    implementation(libs.spock)
    implementation(libs.nativePlatform)
    implementation(libs.commonsLang)
    implementation(libs.commonsIo)
    implementation(libs.jetty)
    implementation(libs.littleproxy)
    implementation(libs.gcs)
    implementation(libs.inject)
    implementation(libs.commonsHttpclient)
    implementation(libs.joda)
    implementation(libs.jacksonCore)
    implementation(libs.jacksonAnnotations)
    implementation(libs.jacksonDatabind)
    implementation(libs.ivy)
    implementation(libs.ant)
    implementation(libs.jgit) {
        because("Some tests require a git reportitory - see AbstractIntegrationSpec.initGitDir(")
    }

    // we depend on both: sshd platforms and libraries
    implementation(libs.sshdCore)
    implementation(platform(libs.sshdCore))
    implementation(libs.sshdScp)
    implementation(platform(libs.sshdScp))
    implementation(libs.sshdSftp)
    implementation(platform(libs.sshdSftp))

    implementation(libs.gson)
    implementation(libs.joda)
    implementation(libs.jsch)
    implementation(libs.jcifs)
    implementation(libs.jansi)
    implementation(libs.ansiControlSequenceUtil)
    implementation(libs.mina)
    implementation(libs.sampleCheck) {
        exclude(module = "groovy-all")
        exclude(module = "slf4j-simple")
    }
    implementation(testFixtures("org.gradle:core"))

    testRuntimeOnly("org.gradle:distributions-core") {
        because("Tests instantiate DefaultClassLoaderRegistry which requires a 'gradle-plugins.properties' through DefaultPluginModuleRegistry")
    }
    integTestDistributionRuntimeOnly("org.gradle:distributions-core")
}

classycle {
    excludePatterns.set(listOf("org/gradle/**"))
}

val prepareVersionsInfo = tasks.register<PrepareVersionsInfo>("prepareVersionsInfo") {
    destFile.set(layout.buildDirectory.file("generated-resources/all-released-versions/all-released-versions.properties"))
    versions.set(moduleIdentity.releasedVersions.map {
        it.allPreviousVersions.joinToString(" ") { it.version }
    })
    mostRecent.set(moduleIdentity.releasedVersions.map { it.mostRecentRelease.version })
    mostRecentSnapshot.set(moduleIdentity.releasedVersions.map { it.mostRecentSnapshot.version })
}

val copyAgpVersionsInfo by tasks.registering(Copy::class) {
    from(rootProject.layout.projectDirectory.file("gradle/dependency-management/agp-versions.properties"))
    into(layout.buildDirectory.dir("generated-resources/agp-versions"))
}

sourceSets.main {
    output.dir(prepareVersionsInfo.map { it.destFile.get().asFile.parentFile })
    output.dir(copyAgpVersionsInfo)
}

@CacheableTask
abstract class PrepareVersionsInfo : DefaultTask() {

    @get:OutputFile
    abstract val destFile: RegularFileProperty

    @get:Input
    abstract val mostRecent: Property<String>

    @get:Input
    abstract val versions: Property<String>

    @get:Input
    abstract val mostRecentSnapshot: Property<String>

    @TaskAction
    fun prepareVersions() {
        val properties = Properties()
        properties["mostRecent"] = mostRecent.get()
        properties["mostRecentSnapshot"] = mostRecentSnapshot.get()
        properties["versions"] = versions.get()
        ReproduciblePropertiesWriter.store(properties, destFile.get().asFile)
    }
}
