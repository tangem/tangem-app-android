import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.tokens"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    implementation(deps.androidx.datastore)
    implementation(deps.arrow.atomic)
    implementation(deps.arrow.core)
    implementation(deps.moshi)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)
    // endregion

    // region Tangem SDK
    implementation(tangemDeps.blockchain)
    // endregion

    // region DI
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core modules
    api(projects.core.datasource)
    api(projects.core.utils)
    // endregion

    // region Data
    api(projects.data.common)
    runtimeOnly(projects.data.networks)
    // endregion

    // region Domain
    api(projects.domain.common)
    api(projects.domain.tokens)
    api(projects.domain.transaction)
    api(projects.domain.walletManager)
    implementation(projects.domain.core)
    implementation(projects.domain.models)
    runtimeOnly(projects.domain.account)
    runtimeOnly(projects.domain.card)
    runtimeOnly(projects.domain.staking)
    // endregion

    // region Domain models
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.walletManager.models)
    // endregion

    // region Common
    implementation(projects.common)
    // endregion

    // region Libs
    api(projects.libs.blockchainSdk)
    // endregion

    // region Tests
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
    // endregion
}