plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.feedback"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Tangem libraries
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)
    // endregion

    // region Core modules
    implementation(projects.core.local)
    api(projects.core.navigation)
    api(projects.core.utils)
    // endregion

    // region Domain
    api(projects.domain.common)
    api(projects.domain.cloudBackup)
    api(projects.domain.feedback)
    implementation(projects.domain.card)
    implementation(projects.domain.cloudBackup)
    implementation(projects.domain.feedback.models)
    implementation(projects.domain.models)
    implementation(projects.domain.socialLogin)
    implementation(projects.domain.walletManager)
    implementation(projects.domain.wallets)
    // endregion

    // region Libs
    implementation(projects.libs.blockchainSdk)
    // endregion
}