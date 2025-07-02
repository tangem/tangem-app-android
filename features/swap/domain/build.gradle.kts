plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.features.domain.swap"
}

dependencies {
    /** Libs */
    implementation(projects.libs.crypto)

    /** DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)

    /** Domain */
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.card)
    implementation(projects.domain.demo)
    implementation(projects.domain.legacy)
    implementation(projects.domain.models)
    implementation(projects.domain.quotes)
    implementation(projects.domain.staking)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.transaction)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)

    implementation(projects.features.swap.domain.api)
    implementation(projects.features.swap.domain.models)
    implementation(projects.libs.blockchainSdk)

    /** Core modules */
    implementation(projects.core.utils)
    implementation(projects.core.ui)
    implementation(projects.core.datasource)

    /** Feature Apis */
    implementation(projects.features.wallet.api)

    /** Other Libraries **/
    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)
    implementation(deps.timber)
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)
    implementation(deps.moshi)
    ksp(deps.moshi.kotlin.codegen)
}