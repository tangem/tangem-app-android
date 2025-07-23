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
    implementation(projects.domain.blockaid.models)
    implementation(projects.domain.core)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.walletConnect.models)
    implementation(projects.domain.transaction)
    implementation(projects.domain.transaction.models)

    /* Project - Core */
    implementation(projects.core.analytics)

    /* Other */
    implementation(deps.moshi.adapters)

    /* Tangem libraries */
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)
}