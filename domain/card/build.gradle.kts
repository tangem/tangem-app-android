plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.card"
}

dependencies {
    implementation(projects.core.analytics.models)
    implementation(projects.core.error)
    implementation(projects.core.error.ext)

    implementation(projects.domain.demo)
    implementation(projects.domain.core)
    implementation(projects.domain.legacy)
    implementation(projects.domain.walletManager) // TODO refactor to use from data module
    implementation(projects.libs.blockchainSdk)
    // TODO: Remove after new card scan result was implemented
    implementation(projects.domain.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)

    implementation(deps.timber)

    implementation(tangemDeps.card.core)
    implementation(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }

    /** Testing libraries */
    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(projects.common.test)
}