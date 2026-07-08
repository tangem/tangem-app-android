plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {

    // region Kotlin
    api(deps.kotlin.datetime)
    // endregion

    // region Domain models
    api(projects.domain.models)
    // endregion

    // region Tests
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.truth)
    // endregion
}