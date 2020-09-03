import gradlebuild.cleanup.WhenNotEmpty

/*
 * Copyright 2014 the original author or authors.
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
plugins {
    id("gradlebuild.distribution.api-java")
}

dependencies {
    implementation("org.gradle:base-services")
    implementation("org.gradle:logging")
    implementation("org.gradle:messaging")
    implementation("org.gradle:file-collections")
    implementation("org.gradle:core-api")
    implementation("org.gradle:core")
    implementation("org.gradle:dependency-management")
    implementation("org.gradle:build-option")

    implementation(libs.groovy)
    implementation(libs.guava)

    testImplementation(testFixtures("org.gradle:resources-http"))

    integTestImplementation("org.gradle:base-services-groovy")
    integTestImplementation(libs.jetbrainsAnnotations)

    integTestDistributionRuntimeOnly("org.gradle:distributions-basics") {
        because("Requires test-kit: 'java-gradle-plugin' is used in integration tests which always adds the test-kit dependency.")
    }
}

testFilesCleanup {
    policy.set(WhenNotEmpty.REPORT)
}

