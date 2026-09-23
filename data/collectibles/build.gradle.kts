plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.data.collectibles"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    implementation(deps.androidx.datastore)
    implementation(deps.androidx.datastore.core)
    implementation(deps.arrow.core)
    implementation(deps.kotlin.serialization)
    ksp(deps.moshi.kotlin.codegen)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core
    api(projects.core.utils)
    implementation(projects.core.error)
    implementation(projects.core.error.ext)
    implementation(projects.core.pagination)
    // endregion

    // region Grow
    implementation(projects.grow.datasource)
    // endregion

    // region Tangem SDK
    implementation(tangemDeps.hot.core)
    implementation(tangemDeps.card.core)
    implementation(tangemDeps.blockchain)
    implementation(projects.libs.blockchainSdk)
    implementation(projects.libs.tangemSdkApi)
    // endregion

    // region Data
    implementation(projects.data.common)
    implementation(projects.data.wallets)
    // endregion

    // region Domain
    api(projects.domain.collectibles)
    implementation(projects.domain.card)
    implementation(projects.domain.walletManager)
    implementation(projects.domain.collectibles.models)
    implementation(projects.domain.core)
    implementation(projects.domain.quotes)
    implementation(projects.domain.common)
    implementation(projects.domain.models)
    implementation(projects.domain.wallets)
    // endregion

    // region Tests
    testImplementation(projects.test.core)
    testImplementation(projects.test.mock)
    testImplementation(projects.libs.crypto)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    // endregion
}