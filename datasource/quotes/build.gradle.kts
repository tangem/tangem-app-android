import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.datasource.quotes"
}

dependencies {

    /** Project - Domain */
    implementation(projects.domain.tokens.models)

    /** Project - Data */
    implementation(projects.core.datasource)
    implementation(projects.data.common)

    /** Project - Utils */
    implementation(projects.core.utils)
    implementation(projects.domain.legacy)
    implementation(projects.libs.blockchainSdk)

    /** Tangem SDKs */
    implementation(deps.tangem.blockchain)
    implementation(deps.tangem.card.core)

    /** AndroidX */
    implementation(deps.androidx.datastore)

    /** DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)

    /** Other */
    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)
    implementation(deps.moshi.kotlin)
    implementation(deps.jodatime)
    implementation(deps.timber)
    implementation(deps.retrofit) // For HttpException
    implementation(deps.androidx.paging.runtime)
    kaptForObfuscatingVariants(deps.moshi.kotlin.codegen)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)
}