plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.dynamicaddresses"
}
dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    api(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }
    api(tangemDeps.card.core)
    // endregion

    // region Domain
    api(projects.domain.models)
    api(projects.domain.walletManager)
    api(projects.domain.wallets)
    // endregion

    // region Domain models
    api(projects.domain.dynamicAddresses.models)
    // endregion

    // region Libs
    implementation(projects.libs.blockchainSdk)
    // endregion

    // region Tests
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    // endregion
}