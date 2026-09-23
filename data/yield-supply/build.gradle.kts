plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.yield.supply"
}
dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.datetime)
    // endregion

    // region AndroidX libraries
    implementation(deps.androidx.datastore)
    // endregion

    // region Other libraries
    implementation(deps.arrow.core)
    // endregion

    // region Tangem SDKs
    implementation(tangemDeps.blockchain)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core
    api(projects.core.analytics)
    implementation(projects.core.configToggles)
    implementation(projects.core.local)
    implementation(projects.store.datasource)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)
    // endregion

    // region Grow
    implementation(projects.grow.datasource)
    // endregion

    // region Domain
    api(projects.domain.transaction)
    api(projects.domain.walletManager)
    api(projects.domain.yieldSupply)
    implementation(projects.domain.legacy)
    implementation(projects.domain.models)
    // endregion

    // region Domain models
    implementation(projects.domain.yieldSupply.models)
    // endregion

    // region Libs
    implementation(projects.libs.blockchainSdk)
    // endregion

    // region tests
    testImplementation(deps.arrow.core)
    testImplementation(projects.test.core)
    // endregion
}