plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}
dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    // endregion

    // region Core modules
    api(projects.core.analytics.models)
    // endregion

    // region Domain
    api(projects.domain.settings)
    api(projects.domain.core)
    // endregion

    // region Domain models
    api(projects.domain.models)
    api(projects.domain.onramp.models)
    api(projects.domain.tokens.models)
    // endregion

    // region Tests
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    // endregion
}