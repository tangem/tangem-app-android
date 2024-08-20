plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.example.data.managetokens"
}

dependencies {

    /** Project - Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.manageTokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)

    /** Project - Data */
    implementation(projects.core.datasource)
    implementation(projects.data.common)

    /** Project - Utils */
    implementation(projects.core.utils)
    implementation(projects.core.pagination)
    implementation(projects.domain.legacy)
    implementation(projects.libs.blockchainSdk)

    /** Tangem SDKs */
    implementation(deps.tangem.blockchain)
    implementation(deps.tangem.card.core)

    /** AndroidX */
    implementation(deps.androidx.datastore)

    /** DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)

    /** Other */
    implementation(deps.moshi.kotlin)
    implementation(deps.timber)
}