plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.polymarket"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.serialization)
    implementation(deps.arrow.core)
    // endregion

    // region Tangem SDK
    implementation(tangemDeps.card.core)
    // AndroidSecureStorageV2 for Polymarket API credentials (see com.tangem.data.polymarket.store).
    implementation(tangemDeps.card.android) {
        exclude(module = "joda-time")
    }
    // Blockchain SDK — Blockchain.Polygon.makeAddressesFromExtendedPublicKey (ERC-55 address).
    implementation(tangemDeps.blockchain)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core
    api(projects.core.datasource)
    api(projects.core.utils)
    implementation(projects.data.common)
    // endregion

    // region Domain
    api(projects.domain.polymarket)
    implementation(projects.domain.wallets)
    implementation(projects.domain.common)
    implementation(projects.domain.card)
    implementation(projects.data.wallets)
    // endregion

    // region tests
    testImplementation(projects.test.core)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(deps.web3j.core)
    // endregion
}