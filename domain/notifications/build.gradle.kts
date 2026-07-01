plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.notifications"
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    implementation(deps.hilt.core)
    // endregion

    // region Core modules
    api(projects.core.utils)
    // endregion

    // region Domain
    api(projects.domain.models)
    // endregion

    // region Domain models
    api(projects.domain.notifications.models)
    // endregion

    // region Libs
    implementation(projects.libs.crypto)
    // endregion

    // region Tests
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    // endregion
}