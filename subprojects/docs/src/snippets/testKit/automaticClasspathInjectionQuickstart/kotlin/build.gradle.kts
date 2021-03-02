// tag::automatic-classpath[]
plugins {
    groovy
    `java-gradle-plugin`
}

dependencies {
    testImplementation("org.spockframework:spock-core:2.0-M4-groovy-3.0") {
        exclude(group = "org.codehaus.groovy")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api")
}
// end::automatic-classpath[]

// TODO The project is currently broken but fails silently (no tests found). When changing to JUnit Platform the build fails because @TempDir doesn't work in Spock 2.0-M4. We expect that this will be fixed in the next milestone.
//tasks.withType<Test>().configureEach {
//    useJUnitPlatform()
//}

repositories {
    mavenCentral()
}
