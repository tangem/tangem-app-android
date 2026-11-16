plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.earn.lending.impl"
}

dependencies {
    /** Feature */
    implementation(projects.features.earn.api)
    implementation(projects.features.earn.product.api)

    /** Common */
    implementation(projects.common)

    /** Core */
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.res)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** Domain */
    implementation(projects.domain.account)
    implementation(projects.domain.core)
    implementation(projects.domain.models)
    implementation(projects.domain.networks)
    implementation(projects.domain.quotes)
    implementation(projects.domain.transaction)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.yieldSupply)
    implementation(projects.domain.yieldSupply.models)

    /** Tangem SDKs */
    implementation(projects.libs.blockchainSdk)
    implementation(projects.libs.crypto)
    implementation(tangemDeps.blockchain)

    /** Kotlin */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.datetime)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(projects.test.core)
    testImplementation(projects.common.test)
    testImplementation(projects.test.mock)
}