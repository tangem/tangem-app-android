plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.qrscanning"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region SDK
    implementation(tangemDeps.blockchain)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core modules
    implementation(projects.core.ui)
    // endregion

    // region Domain
    api(projects.domain.qrScanning)
    implementation(projects.domain.models)
    implementation(projects.domain.qrScanning.models)
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