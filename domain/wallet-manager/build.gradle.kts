plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.walletmanager"
}

dependencies {
    /** Domain models */
    api(projects.domain.walletManager.models)

    /** Project - Domain */
    implementation(projects.core.utils)
    implementation(projects.core.error)
    api(projects.domain.models)
    implementation(projects.domain.core)
    implementation(projects.domain.demo.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.txhistory.models)

    /** Tangem libs */
    implementation(tangemDeps.card.core)

    /** Libs - Other */
    implementation(projects.libs.blockchainSdk)
    implementation(tangemDeps.blockchain)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    ksp(deps.moshi.kotlin.codegen)

    /** Testing libraries */
    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(projects.common.test)
}