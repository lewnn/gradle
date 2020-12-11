plugins {
    id("gradlebuild.distribution.api-java")
}

gradlebuildJava.usedInWorkers()

dependencies {
    implementation(project(":base-services"))

    implementation(libs.fastutil)
    implementation(libs.slf4jApi)
    implementation(libs.guava)
    implementation(libs.kryo)

    testImplementation(testFixtures(project(":core")))

    testFixturesImplementation(project(":base-services"))
    testFixturesImplementation(libs.slf4jApi)

    integTestDistributionRuntimeOnly("org.gradle:distributions-core")
}
