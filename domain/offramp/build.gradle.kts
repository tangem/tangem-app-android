plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    /** Domain modules */
    api(projects.domain.core)
    api(projects.domain.models)

    /** Test libraries */
    testImplementation(projects.test.core)
    testRuntimeOnly(deps.test.junit5.engine)
}
