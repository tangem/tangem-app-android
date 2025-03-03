import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.swap.data"
}

dependencies {

    /** AndroidX */
    implementation(deps.androidx.datastore)

    /** Project*/
    implementation(projects.core.datasource)
    implementation(projects.core.utils)
    implementation(projects.features.swap.domain)
    implementation(projects.features.swap.domain.models)
    implementation(projects.features.swap.domain.api)

    /** Network */
    implementation(deps.retrofit)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.arrow.core)
    ksp(deps.moshi.kotlin.codegen)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)

    /** Domain */
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.legacy)
    implementation(projects.libs.blockchainSdk)
    implementation(projects.domain.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)

    /** Data */
    implementation(projects.data.common)

    /** Tangem SDKs */
    implementation(tangemDeps.blockchain)

    /** Others */
    implementation(deps.timber)

    /** DI */
    implementation(deps.hilt.android)

    kapt(deps.hilt.kapt)
}