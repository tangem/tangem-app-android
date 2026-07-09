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

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    implementation(deps.arrow.core)
    implementation(deps.jodatime)
    implementation(tangemDeps.blockchain)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core modules
    api(projects.core.analytics)
    api(projects.core.analytics.models)
    api(projects.core.datasource)
    api(projects.core.utils)
    implementation(projects.core.pagination)
    // endregion

    // region Data
    api(projects.data.common)
    // endregion

    // region Domain
    api(projects.domain.common)
    api(projects.domain.markets)
    implementation(projects.domain.models)
    runtimeOnly(projects.domain.tokens)
    // endregion

    // region Domain models
    implementation(projects.domain.markets.models)
    // endregion

    // region Libs
    api(projects.libs.blockchainSdk)
    implementation(projects.libs.crypto)
    // endregion

    // region Tests dependencies
    testImplementation(projects.test.core)
    // endregion
}
