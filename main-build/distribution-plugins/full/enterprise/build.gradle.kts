/*
 * Copyright 2020 the original author or authors.
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
    api("org.gradle:base-services") // leaks BuildOperationNotificationListener on API

    implementation(libs.jsr305)
    implementation(libs.inject)
    implementation("org.gradle:logging")
    implementation("org.gradle:core-api")
    implementation("org.gradle:core")
    implementation("org.gradle:launcher")
    implementation("org.gradle:snapshots")

    integTestImplementation("org.gradle:internal-testing")
    integTestImplementation("org.gradle:internal-integ-testing")

    // Dependencies of the integ test fixtures
    integTestImplementation("org.gradle:build-option")
    integTestImplementation("org.gradle:messaging")
    integTestImplementation("org.gradle:persistent-cache")
    integTestImplementation("org.gradle:native")
    integTestImplementation(libs.guava)

    integTestDistributionRuntimeOnly("org.gradle:distributions-full")
}
