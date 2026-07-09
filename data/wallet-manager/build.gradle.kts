plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.walletmanager"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    implementation(deps.arrow.core)
    implementation(deps.moshi)
    // endregion

    // region Tangem libraries
    api(tangemDeps.blockchain)
    api(tangemDeps.card.core)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core
    api(projects.core.datasource)
    api(projects.core.utils)
    // endregion

    // region Domain
    api(projects.domain.common)
    api(projects.domain.models)
    api(projects.domain.transaction)
    implementation(projects.domain.card)
    implementation(projects.domain.walletManager)
    implementation(projects.domain.wallets)
    // endregion

    // region Domain models
    implementation(projects.domain.demo.models)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.walletManager.models)
    // endregion

    // region Libs
    api(projects.libs.blockchainSdk)
    // endregion

    // region Testing libraries
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
    // endregion
}