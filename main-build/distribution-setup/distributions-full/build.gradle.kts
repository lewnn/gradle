import gradlebuild.run.tasks.RunEmbeddedGradle

plugins {
    id("gradlebuild.distribution.packaging")
    id("gradlebuild.verify-build-environment")
    id("gradlebuild.install")
}

dependencies {
    coreRuntimeOnly(platform("org.gradle:core-platform"))

    pluginsRuntimeOnly(platform("org.gradle:distributions-publishing"))
    pluginsRuntimeOnly(platform("org.gradle:distributions-jvm"))
    pluginsRuntimeOnly(platform("org.gradle:distributions-native"))

    pluginsRuntimeOnly("org.gradle:plugin-development")
    pluginsRuntimeOnly("org.gradle:build-init")
    pluginsRuntimeOnly("org.gradle:build-profile")
    pluginsRuntimeOnly("org.gradle:antlr")
    pluginsRuntimeOnly("org.gradle:enterprise")

    // The following are scheduled to be removed from the distribution completely in Gradle 7.0
    pluginsRuntimeOnly("org.gradle:javascript")
    pluginsRuntimeOnly("org.gradle:platform-play")
    pluginsRuntimeOnly("org.gradle:ide-play")
}

tasks.register<RunEmbeddedGradle>("runDevGradle") {
    group = "verification"
    description = "Runs an embedded Gradle using the partial distribution for ${project.path}."
    gradleClasspath.from(configurations.runtimeClasspath.get(), tasks.runtimeApiInfoJar)
}

// This is required for the separate promotion build and should be adjusted there in the future
tasks.register<Copy>("copyDistributionsToRootBuild") {
    dependsOn("buildDists")
    from(layout.buildDirectory.dir("distributions"))
    into(rootProject.layout.buildDirectory.dir("distributions"))
}
