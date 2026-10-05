plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.offramp"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.serialization)
    // endregion

    // region Other libraries
    implementation(deps.androidx.datastore)
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
    implementation(projects.core.local)
    implementation(projects.core.ui)
    // endregion

    // region Grow modules
    implementation(projects.grow.datasource)
    // endregion

    // region Domain modules
    api(projects.domain.offramp)
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