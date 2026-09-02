import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.nft"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    implementation(deps.arrow.core)
    api(deps.moshi)
    implementation(deps.androidx.datastore.base)
    implementation(deps.androidx.datastore.core)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)
    // endregion

    // region Libs - Tangem
    implementation(tangemDeps.blockchain)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Project - Core
    api(projects.core.utils)
    implementation(projects.core.local)
    implementation(projects.core.remote)
    implementation(projects.core.res)
    // endregion

    // region Project - Data
    api(projects.data.common)
    // endregion

    // region Project - Domain
    api(projects.domain.common)
    api(projects.domain.walletManager)
    implementation(projects.domain.card)
    implementation(projects.domain.models)
    implementation(projects.domain.nft)
    implementation(projects.domain.nft.models)
    // endregion

    // region Project - Libs
    api(projects.libs.blockchainSdk)
    // endregion

    // region Tests
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(projects.common.test)
    // endregion
}