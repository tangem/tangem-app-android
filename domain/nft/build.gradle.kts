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
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)

    implementation(projects.core.utils)

    implementation(projects.domain.core)
    implementation(projects.domain.models)
    implementation(projects.domain.nft.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)
}