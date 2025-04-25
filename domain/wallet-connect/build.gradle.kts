plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.walletconnect"
}

dependencies {
    /* Project - Domain */
    implementation(projects.domain.core)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.walletConnect.models)
    implementation(projects.domain.blockaid.models)

    /* Other */
    implementation(deps.moshi.adapters)

    /* Tangem libraries */
    implementation(tangemDeps.blockchain)
}