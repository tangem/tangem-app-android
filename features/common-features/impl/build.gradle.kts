plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.commonfeatures.impl"
}
dependencies {
    /** Api */
    api(projects.features.commonFeatures.api)
    api(projects.features.tokenRecieve.api)
    api(projects.features.wallet.api)

    /** Core modules */
    api(projects.core.analytics)
    api(projects.core.decompose)
    api(projects.core.ui)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)
    implementation(projects.core.configToggles)
    implementation(projects.core.pagination)

    /** Domain */
    api(projects.domain.account.status)
    api(projects.domain.appCurrency)
    api(projects.domain.appCurrency.models)
    api(projects.domain.balanceHiding)
    api(projects.domain.manageTokens)
    api(projects.domain.markets)
    api(projects.domain.markets.models)
    api(projects.domain.models)
    api(projects.domain.transaction)
    api(projects.domain.wallets)
    implementation(projects.domain.account)
    implementation(projects.domain.card)
    implementation(projects.domain.core)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.yieldSupply.models)

    /** Tangem libraries */
    implementation(tangemDeps.card.core)
    implementation(tangemDeps.blockchain)

    /** Common */
    api(projects.common.routing)
    api(projects.common.ui)
    api(projects.common.uiMarkets)
    implementation(projects.common)
    implementation(projects.common.uiCharts)

    /** Libs */
    api(projects.libs.blockchainSdk)
    implementation(projects.libs.crypto)

    /** AndroidX libraries */
    implementation(deps.androidx.appCompat)
    implementation(deps.lifecycle.runtime.ktx)

    /** Compose libraries */
    api(deps.compose.animation)
    api(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.decompose.ext.compose)

    /** Other libraries */
    api(deps.kotlin.coroutines)
    api(deps.kotlin.immutable.collections)
    implementation(deps.arrow.core)
    implementation(deps.haze)
    implementation(deps.kotlin.serialization)
    implementation(deps.lifecycle.compose)
    implementation(deps.firebase.crashlytics)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
}