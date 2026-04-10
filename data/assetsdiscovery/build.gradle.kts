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
    api(projects.domain.assetsdiscovery)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.models)
    implementation(projects.domain.walletManager)
    implementation(projects.domain.wallets)
    implementation(projects.data.common)
    implementation(projects.data.walletManager)
    implementation(projects.libs.blockchainSdk)
    implementation(projects.libs.tangemSdkApi)
    implementation(projects.core.datasource)
    implementation(projects.core.utils)

    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)

    implementation(deps.androidx.datastore)

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    implementation(deps.kotlin.coroutines)
    implementation(deps.moshi)
}