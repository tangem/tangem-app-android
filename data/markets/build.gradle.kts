import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.markets"
}

dependencies {
    implementation(projects.core.datasource)
    implementation(projects.core.utils)
    implementation(projects.core.pagination)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.configToggles)

    implementation(projects.domain.legacy)
    implementation(projects.domain.markets)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens.models)

    implementation(projects.data.common)

    implementation(projects.libs.blockchainSdk)

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Others dependencies
    implementation(deps.kotlin.coroutines)
    implementation(deps.jodatime)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.timber)
    implementation(deps.tangem.blockchain)
    kaptForObfuscatingVariants(deps.moshi.kotlin.codegen)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)
    // endregion
}
