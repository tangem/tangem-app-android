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

    // region AndroidX libraries
    implementation(deps.androidx.datastore)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Tangem libraries
    implementation(deps.tangem.blockchain)
    implementation(deps.tangem.card.core)
    // endregion

    // Other libraries
    implementation(deps.timber)
    // endregion

    // region Core modules
    implementation(projects.core.datasource)
    // endregion

    implementation(projects.core.utils)

    implementation(projects.domain.feedback)
    implementation(projects.domain.legacy)
    implementation(projects.domain.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)

    implementation(projects.libs.blockchainSdk)
}