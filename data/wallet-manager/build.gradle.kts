plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.walletmanager"
}

dependencies {
    /** Tangem libraries */
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)

    /** Core */
    implementation(projects.core.datasource)
    implementation(projects.core.utils)
    implementation(projects.core.error)
    implementation(projects.core.error.ext)

    /** Domain */
    implementation(projects.domain.wallets)
    implementation(projects.domain.walletManager)
    implementation(projects.domain.demo)
    implementation(projects.domain.card)
    api(projects.domain.models)

    /** Domain models */
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.transaction.models)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Other deps */
    implementation(projects.libs.blockchainSdk)
    implementation(deps.androidx.datastore)
    implementation(deps.arrow.core)
    implementation(deps.timber)

    /** Testing libraries */
    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(projects.common.test)
}