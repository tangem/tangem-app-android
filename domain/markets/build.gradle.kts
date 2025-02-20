plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.markets"
}


dependencies {
    /* Domain */
    api(projects.domain.appCurrency.models)
    api(projects.domain.card)
    api(projects.domain.core)
    api(projects.domain.markets.models)
    api(projects.domain.wallets.models)
    api(projects.domain.models)
    api(projects.domain.legacy)
    api(projects.domain.wallets)

    implementation(projects.domain.tokens.models)
    implementation(projects.domain.tokens)

    api(projects.core.pagination)

    /* Libs */
    api(projects.libs.blockchainSdk)

    /* SDK */
    implementation(tangemDeps.blockchain)

    /* Utils */
    implementation(deps.kotlin.serialization)
    implementation(projects.core.utils)
}
