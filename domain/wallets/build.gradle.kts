plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.wallets"
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    api(tangemDeps.blockchain) // android-library
    api(tangemDeps.card.core)
    api(tangemDeps.hot.core)
    implementation(deps.hilt.android)
    // endregion

    // region Firebase
    implementation(platform(deps.firebase.bom))
    implementation(deps.firebase.analytics)
    // endregion

    // region Core modules
    api(projects.core.analytics)
    api(projects.core.analytics.models)
    api(projects.core.utils)
    // endregion

    // region Domain
    api(projects.domain.account)
    api(projects.domain.card)
    api(projects.domain.common)
    api(projects.domain.hotWallet)
    api(projects.domain.walletManager)
    api(projects.domain.core)
    implementation(projects.domain.legacy)
    // endregion

    // region Domain models
    api(projects.domain.models)
    api(projects.domain.wallets.models)
    implementation(projects.domain.demo.models)
    implementation(projects.domain.notifications.models)
    // endregion

    // region Libs
    implementation(projects.libs.blockchainSdk)
    // endregion

    // region Common
    implementation(projects.common)
    // endregion

    // region Tests
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
    // endregion
}