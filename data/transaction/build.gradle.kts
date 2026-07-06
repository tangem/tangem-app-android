plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.transaction"
}
dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    implementation(deps.androidx.core.ktx)
    // endregion

    // region Tangem SDKs
    api(tangemDeps.blockchain)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core
    api(projects.core.configToggles)
    api(projects.core.datasource)
    api(projects.core.utils)
    // endregion

    // region Common
    api(projects.data.common)
    // endregion

    // region Domain
    api(projects.domain.models)
    api(projects.domain.transaction)
    api(projects.domain.walletManager)
    implementation(projects.domain.demo)
    // endregion

    // region Domain models
    api(projects.domain.wallets.models)
    implementation(projects.domain.demo.models)
    implementation(projects.domain.transaction.models)
    // endregion

    // region Libs
    api(projects.libs.blockchainSdk)
    implementation(projects.libs.crypto)
    // endregion

    // region tests
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    // endregion
}