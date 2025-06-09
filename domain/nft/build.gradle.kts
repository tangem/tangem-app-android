plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.nft"
}

dependencies {
    // region Project – Core
    implementation(projects.core.analytics.models)
    implementation(projects.core.utils)
    // endregion

    // region Project – Domain
    implementation(projects.domain.core)
    implementation(projects.domain.models)
    implementation(projects.domain.networks)
    implementation(projects.domain.nft.models)
    implementation(projects.domain.quotes)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)
    // endregion

    // region Others
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    // endregion
}