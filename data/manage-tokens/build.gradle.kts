import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

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
    implementation(projects.domain.demo)
    implementation(projects.domain.models)
    implementation(projects.domain.manageTokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.legacy)

    /** Project - Data */
    implementation(projects.core.datasource)
    implementation(projects.data.common)
    implementation(projects.data.tokens)

    /** Project - Utils */
    implementation(projects.core.utils)
    implementation(projects.core.pagination)
    implementation(projects.domain.legacy)
    implementation(projects.libs.blockchainSdk)

    /** Tangem SDKs */
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)

    /** AndroidX */
    implementation(deps.androidx.datastore)

    /** DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)

    /** Other */
    implementation(deps.moshi.kotlin)
    implementation(deps.timber)
    kaptForObfuscatingVariants(deps.moshi.kotlin.codegen)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)
}