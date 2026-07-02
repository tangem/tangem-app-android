plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.data.swap"
}
dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.moshi)
    implementation(deps.androidx.datastore)
    implementation(deps.arrow.core)
    implementation(deps.jodatime)
    ksp(deps.moshi.kotlin.codegen)
    // endregion

    // region Tangem SDK
    implementation(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core
    api(projects.core.configToggles)
    api(projects.core.datasource)
    api(projects.core.utils)
    // endregion

    // region Data
    api(projects.data.common)
    implementation(projects.data.express)
    // endregion

    // region Domain
    api(projects.domain.account)
    api(projects.domain.express)
    api(projects.domain.models)
    api(projects.domain.quotes)
    api(projects.domain.swap)
    implementation(projects.domain.core)
    implementation(projects.domain.tokens)
    runtimeOnly(projects.domain.staking)
    runtimeOnly(projects.domain.wallets)
    // endregion

    // region Domain models
    api(projects.domain.swap.models)
    implementation(projects.domain.express.models)
    // endregion

    // region Libs
    implementation(projects.libs.blockchainSdk)
    implementation(projects.libs.crypto)
    // endregion

    // region Test
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(projects.common.test)
    // endregion
}