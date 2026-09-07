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

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    implementation(deps.arrow.core)
    implementation(deps.moshi)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)
    // endregion

    // region Tangem SDKs
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)
    // endregion

    // region DI
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
    // endregion

    // region Project - Core
    implementation(projects.core.local)
    implementation(projects.store.datasource)
    api(projects.core.utils)
    implementation(projects.core.pagination)
    // endregion

    // region Project - Data
    api(projects.data.common)
    runtimeOnly(projects.data.tokens)
    // endregion

    // region Project - Domain
    api(projects.domain.common)
    api(projects.domain.manageTokens)
    implementation(projects.domain.card)
    implementation(projects.domain.jointAccount)
    implementation(projects.domain.models)
    implementation(projects.domain.wallets)
    runtimeOnly(projects.domain.account)
    // endregion

    // region Project - Domain models
    implementation(projects.domain.manageTokens.models)
    // endregion

    // region Project - Libs
    api(projects.libs.blockchainSdk)
    implementation(projects.libs.crypto)
    // endregion

    // region Tests
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
    // endregion
}