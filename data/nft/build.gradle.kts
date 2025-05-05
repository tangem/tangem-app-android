import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.data.nft"
}

dependencies {

    /** Project - Data */
    implementation(projects.core.datasource)
    implementation(projects.data.common)

    /** Project - Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.nft)
    implementation(projects.domain.nft.models)

    /** Project - Utils */
    implementation(projects.core.utils)
    implementation(projects.domain.legacy)
    implementation(projects.libs.blockchainSdk)

    /** Feature Api modules */
    implementation(projects.features.nft.api)

    /** Libs - Other */
    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)
    implementation(deps.arrow.fx)
    implementation(deps.jodatime)
    implementation(deps.timber)
    implementation(deps.androidx.paging.runtime)
    implementation(deps.moshi.kotlin)
    ksp(deps.moshi.kotlin.codegen)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)

    /** Libs - Tangem */
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)

    /** DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
}