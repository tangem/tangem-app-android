plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.promo"
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    api(deps.arrow.core)
    // endregion

    // region Core modules
    api(projects.core.utils)
    // endregion

    // region Domain models
    api(projects.domain.models)
    api(projects.domain.promo.models)
    // endregion

    // region Tests
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(projects.test.core)
    // endregion
}