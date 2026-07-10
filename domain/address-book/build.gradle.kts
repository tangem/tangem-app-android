plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.addressbook"
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    api(deps.kotlin.serialization)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    api(deps.jodatime)
    // endregion

    // region Core modules
    implementation(projects.core.utils)
    // endregion

    // region Domain
    api(projects.domain.common)
    api(projects.domain.tokens)
    api(projects.domain.transaction)
    // endregion

    // region Domain models
    api(projects.domain.models)
    // endregion

    // region Tests
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(projects.common.test)
    // endregion
}