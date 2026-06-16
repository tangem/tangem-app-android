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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    /** Libs */
    implementation(projects.libs.crypto)

    /** DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)

    /** Domain */
    implementation(projects.domain.swap.models)
    implementation(projects.domain.swap)
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.card)
    implementation(projects.domain.demo)
    implementation(projects.domain.legacy)
    implementation(projects.domain.models)
    implementation(projects.domain.quotes)
    implementation(projects.domain.staking)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.transaction)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.express.models)
    implementation(projects.domain.account)
    implementation(projects.domain.account.status)
    implementation(projects.domain.visa)
    implementation(projects.domain.visa.models)
    implementation(projects.domain.balanceHiding)

    /** Core modules */
    implementation(projects.core.utils)
    implementation(projects.core.ui)
    implementation(projects.core.datasource)
    implementation(projects.core.abTests)

    /** Feature Apis */
    implementation(projects.features.wallet.api)
    implementation(projects.features.swap.api)
    implementation(projects.features.swap.domain.api)
    implementation(projects.features.swap.domain.models)
    implementation(projects.features.sendV2.api)
    implementation(projects.libs.blockchainSdk)

    /** Other Libraries **/
    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)
    implementation(deps.moshi)
    ksp(deps.moshi.kotlin.codegen)

    /** Test */
    testImplementation(projects.test.core)
    testRuntimeOnly(deps.test.junit5.engine)
}