plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}
dependencies {
    /** Domain modules */
    api(projects.domain.core)
    api(projects.domain.models)

    /** Test libraries */
    testImplementation(projects.test.core)
}
