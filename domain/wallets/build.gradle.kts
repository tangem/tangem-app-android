plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.wallets"
}

dependencies {

    // region Core modules
    implementation(projects.core.res)
    implementation(projects.core.utils)
    // endregion

    // region Domain modules
    api(projects.domain.core)
    implementation(projects.domain.legacy)
    implementation(projects.libs.blockchainSdk)
    implementation(projects.libs.tangemSdkApi)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.notifications.models)
    // endregion

    // region Tangem libraries
    implementation(tangemDeps.blockchain) // android-library
    implementation(tangemDeps.card.core)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // end

    // region Tests
    testImplementation(deps.test.junit)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.mockk)
    // end
}