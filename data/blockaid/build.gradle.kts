plugins {
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.android.library)
    id("configuration")
}

android {
    namespace = "com.tangem.data.blockaid"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    // endregion

    // region Tangem libraries
    api(tangemDeps.blockchain)
    // endregion

    // region DI
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core modules
    api(projects.core.datasource)
    api(projects.core.utils)
    // endregion

    // region Domain
    api(projects.domain.blockaid)
    api(projects.domain.blockaid.models)
    api(projects.domain.models)
    // endregion

    // region Libs
    implementation(projects.libs.blockchainSdk)
    // endregion

    // region Tests
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    // endregion
}