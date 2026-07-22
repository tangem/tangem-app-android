plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.walletmanager"
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    api(tangemDeps.blockchain)
    // endregion

    // region Core modules
    api(projects.core.utils)
    // endregion

    // region Domain models
    api(projects.domain.models)
    api(projects.domain.transaction.models)
    api(projects.domain.txhistory.models)
    api(projects.domain.walletManager.models)
    // endregion

    // region Libs
    api(projects.libs.blockchainSdk)
    // endregion

    // region Tests
    testImplementation(deps.test.mockk)
    // endregion
}