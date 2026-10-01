plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.moonpay"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Tangem SDK
    implementation(tangemDeps.card.core)
    implementation(tangemDeps.blockchain)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core modules
    implementation(projects.core.utils)
    // endregion

    // region Grow modules
    implementation(projects.grow.datasource)
    // endregion

    // region Domain modules
    api(projects.domain.moonpay)
    implementation(projects.domain.card)
    implementation(projects.domain.wallets)
    implementation(projects.domain.legacy)
    // endregion

    // region Domain models
    api(projects.domain.models)
    // endregion

    // region Libs
    implementation(projects.libs.blockchainSdk)
    // endregion

    // region Tests
    testImplementation(projects.test.core)
    testImplementation(projects.common.test)
    // endregion
}