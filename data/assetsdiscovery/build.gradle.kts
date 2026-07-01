plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.assetsdiscovery"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.moshi)
    implementation(deps.androidx.datastore)
    // endregion

    // region Tangem SDK
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core modules
    api(projects.core.datasource)
    api(projects.core.utils)
    // endregion

    // region Data
    api(projects.data.common)
    implementation(projects.data.walletManager)
    // endregion

    // region Domain
    api(projects.domain.assetsdiscovery)
    api(projects.domain.common)
    api(projects.domain.models)
    implementation(projects.domain.wallets)
    runtimeOnly(projects.domain.tokens)
    // endregion

    // region Libs
    api(projects.libs.blockchainSdk)
    // endregion
}