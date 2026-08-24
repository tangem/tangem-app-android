plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Domain
    api(projects.domain.markets.models)
    // endregion

    // region Test
    testImplementation(projects.test.core)
    // endregion
}