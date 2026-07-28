plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

dependencies {

    // region DI
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
    // endregion

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Tangem
    implementation(tangemDeps.card.core) // for calculating user id hash
    // endregion

    // region Core modules
    api(projects.core.analytics.models)
    // Core shouldn't depend on core, but with utils and logging it's necessary.
    implementation(projects.core.utils)
    // endregion

    // region Domain
    api(projects.domain.analytics)
    // endregion

    // region Domain models
    api(projects.domain.models)
    // endregion

    // region Tests
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    // endregion
}