plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {

    // region Domain
    api(projects.domain.core)
    api(projects.domain.models)
    // endregion

    // region Tests
    testImplementation(projects.test.core)
    // endregion
}