import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.data.visa"
}

dependencies {

    /** Project - Data */
    implementation(projects.core.datasource)
    implementation(projects.core.error)
    implementation(projects.core.error.ext)
    implementation(projects.core.security)
    implementation(projects.data.common)

    /** Project - Domain */
    implementation(projects.domain.visa)
    implementation(projects.domain.card)
    implementation(projects.domain.wallets)
    implementation(projects.domain.legacy)
    implementation(projects.domain.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.networks)
    implementation(projects.domain.walletManager)
    implementation(projects.domain.quotes)
    implementation(projects.domain.common)

    /** Feature API - remove after removing [HotWalletFeatureToggles] */
    implementation(projects.features.hotWallet.api)

    /** Feature API - remove after removing [TangemPayFeatureToggles] */
    implementation(projects.features.tangempay.details.api)

    /** Project - Utils */
    implementation(projects.core.utils)
    implementation(projects.domain.legacy)
    implementation(projects.libs.blockchainSdk)

    /** Project - Libs */
    implementation(projects.libs.visa)

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
    implementation(tangemDeps.hot.core)
    implementation(projects.libs.tangemSdkApi)

    /** DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
}