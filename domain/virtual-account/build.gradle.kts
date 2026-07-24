plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.virtualaccount"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    implementation(deps.arrow.core)
    // endregion

    // region Core modules
    api(projects.core.security)
    // endregion

    // region Domain
    api(projects.domain.common)
    api(projects.domain.visa)
    // endregion

    // region Domain models
    api(projects.domain.models)
    api(projects.domain.virtualAccount.models)
    // endregion

    // region Tests
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(projects.common.test)
    testImplementation(projects.domain.card)
    testImplementation(projects.domain.visa.models)
    // endregion
}