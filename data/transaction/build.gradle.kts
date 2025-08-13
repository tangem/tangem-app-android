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

    /** Tangem SDKs */
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)

    /** Core */
    implementation(projects.core.datasource)
    implementation(projects.core.utils)

    /** Domain */
    implementation(projects.domain.transaction)
    implementation(projects.domain.legacy)
    implementation(projects.domain.walletManager)
    implementation(projects.libs.blockchainSdk)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.transaction.models)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Other */
    implementation(deps.timber)
}