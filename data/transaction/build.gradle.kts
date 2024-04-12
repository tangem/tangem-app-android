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
    implementation(deps.tangem.blockchain)

    /** Core */
    implementation(projects.core.utils)

    /** Domain */
    implementation(projects.domain.transaction)
    implementation(projects.domain.legacy)
    implementation(projects.libs.blockchainSdk)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.tokens.models)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}