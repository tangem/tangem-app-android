plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.wallet"
}

dependencies {
    implementation(projects.data.common)

    /** Tangem libraries */
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)
    implementation(tangemDeps.hot.core)
    implementation(projects.libs.tangemSdkApi)
    implementation(projects.libs.blockchainSdk)

    /** Core */
    implementation(projects.core.datasource)
    implementation(projects.core.utils)

    /** Domain */
    implementation(projects.domain.account)
    implementation(projects.domain.card)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Other deps */
    implementation(deps.androidx.datastore)
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.retrofit)
    implementation(deps.timber)

    /** tests */
    testImplementation(projects.common.test)
    testImplementation(deps.test.junit)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.mockk)
}