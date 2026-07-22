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
    namespace = "com.tangem.data.onramp"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.serialization)
    // endregion

    // region Other libraries
    api(deps.moshi)
    implementation(deps.androidx.datastore)
    implementation(deps.arrow.core)
    implementation(deps.jodatime)
    ksp(deps.moshi.kotlin.codegen)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)
    // endregion

    // region Tangem SDK
    api(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }
    implementation(tangemDeps.card.core)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core modules
    api(projects.core.analytics)
    api(projects.core.datasource)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)
    // endregion

    // region Data modules
    api(projects.data.common)
    // endregion

    // region Domain modules
    api(projects.domain.common)
    api(projects.domain.models)
    api(projects.domain.onramp)
    api(projects.domain.txhistory)
    api(projects.domain.walletManager)
    implementation(projects.domain.card)
    runtimeOnly(projects.domain.account)
    // endregion

    // region Domain models
    implementation(projects.domain.express.models)
    implementation(projects.domain.onramp.models)
    implementation(projects.domain.tokens.models)
    // endregion

    // region Libs
    api(projects.libs.blockchainSdk)
    // endregion
}
