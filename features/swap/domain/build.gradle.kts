plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.features.domain.swap"

    testOptions {
        unitTests.isIncludeAndroidResources = false
    }
}
dependencies {
    /** Libs */
    implementation(projects.libs.crypto)

    /** DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)

    /** Domain */
    api(projects.domain.account.status)
    api(projects.domain.appCurrency)
    api(projects.domain.appCurrency.models)
    api(projects.domain.balanceHiding)
    api(projects.domain.demo)
    api(projects.domain.express.models)
    api(projects.domain.legacy)
    api(projects.domain.models)
    api(projects.domain.notifications)
    api(projects.domain.quotes)
    api(projects.domain.swap)
    api(projects.domain.swap.models)
    api(projects.domain.tokens)
    api(projects.domain.tokens.models)
    api(projects.domain.transaction)
    api(projects.domain.transaction.models)
    api(projects.domain.visa)
    api(projects.domain.visa.models)
    api(projects.domain.walletManager)
    api(projects.domain.yieldSupply)

    /** Common modules */
    api(projects.common.ui)

    /** Core modules */
    api(projects.core.abTests)
    api(projects.core.datasource)
    api(projects.core.ui)
    implementation(projects.core.utils)
    implementation(projects.core.error)

    /** Feature Apis */
    api(projects.features.swap.api)
    implementation(projects.features.send.api)
    implementation(projects.libs.blockchainSdk)

    /** Other Libraries **/
    api(deps.arrow.core)
    api(deps.jodatime)
    api(deps.kotlin.coroutines)
    api(deps.moshi)
    api(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)
    ksp(deps.moshi.kotlin.codegen)

    /** Test */
    testRuntimeOnly(projects.domain.account)
    testRuntimeOnly(projects.domain.card)
    testRuntimeOnly(projects.domain.staking)
    testRuntimeOnly(projects.domain.wallets)
    testImplementation(deps.test.junit)
    testImplementation(projects.test.core)
}