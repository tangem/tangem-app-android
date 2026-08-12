plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.data.wallet"
}
dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Other deps
    api(deps.moshi)
    ksp(deps.moshi.kotlin.codegen)
    implementation(deps.androidx.datastore)
    implementation(deps.arrow.core)
    // endregion

    // region Tangem libraries
    api(tangemDeps.blockchain)
    api(tangemDeps.card.core)
    api(tangemDeps.hot.core)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core
    api(projects.core.datasource)
    api(projects.core.utils)
    implementation(projects.domain.demo)
    // endregion

    // region Data
    api(projects.data.common)
    // endregion

    // region Domain
    api(projects.domain.common)
    api(projects.domain.dynamicAddresses)
    api(projects.domain.models)
    api(projects.domain.settings)
    api(projects.domain.wallets)
    implementation(projects.domain.card)
    implementation(projects.domain.wallets.models)
    runtimeOnly(projects.domain.account)
    // endregion

    // region Libs
    api(projects.libs.tangemSdkApi)
    implementation(projects.libs.blockchainSdk)
    // endregion

    /** tests */
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
}