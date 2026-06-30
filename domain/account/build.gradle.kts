plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}
dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    api(deps.kotlin.serialization)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    // endregion

    // region Core modules
    api(projects.core.utils)
    // endregion

    // region Domain
    runtimeOnly(projects.domain.common)
    api(projects.domain.core)
    // endregion

    // region Domain models
    api(projects.domain.models)
    // endregion

    // region Tests
    testImplementation(projects.test.core)
    testImplementation(projects.test.mock)
    // endregion
}