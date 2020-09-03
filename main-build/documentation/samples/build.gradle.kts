import gradlebuild.cleanup.WhenNotEmpty
import gradlebuild.integrationtests.integrationTestUsesSampleDir

plugins {
    id("gradlebuild.internal.java")
}

dependencies {
    integTestImplementation("org.gradle:base-services")
    integTestImplementation("org.gradle:core-api")
    integTestImplementation("org.gradle:process-services")
    integTestImplementation("org.gradle:persistent-cache")
    integTestImplementation(libs.groovy)
    integTestImplementation(libs.slf4jApi)
    integTestImplementation(libs.guava)
    integTestImplementation(libs.ant)
    integTestImplementation(libs.sampleCheck) {
        exclude(group = "org.codehaus.groovy", module = "groovy-all")
        exclude(module = "slf4j-simple")
    }
    integTestImplementation(testFixtures("org.gradle:core"))

    integTestDistributionRuntimeOnly("org.gradle:distributions-full")
}

testFilesCleanup {
    policy.set(WhenNotEmpty.REPORT)
}

integrationTestUsesSampleDir("subprojects/core-api/src/main/java", "subprojects/core/src/main/java")
