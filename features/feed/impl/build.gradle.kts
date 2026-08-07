plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.feed.impl"

    packaging {
        resources {
            merges += "paymentrequest.proto"
        }
    }
}
dependencies {
    /* Project - API */
    api(projects.features.commonFeatures.api)
    api(projects.features.feed.api)
    api(projects.features.forYou.api)
    api(projects.features.promoBanners.api)
    api(projects.features.tokenRecieve.api)
    api(projects.features.marketing.api)

    /* Data */
    implementation(projects.data.common)

    /* Domain */
    api(projects.domain.account.status)
    api(projects.domain.appCurrency)
    api(projects.domain.balanceHiding)
    api(projects.domain.common)
    api(projects.domain.earn)
    api(projects.domain.feedback)
    api(projects.domain.markets)
    api(projects.domain.models)
    api(projects.domain.news)
    api(projects.domain.search)
    api(projects.domain.settings)
    api(projects.domain.tokens)
    api(projects.domain.transaction)
    api(projects.domain.wallets)
    api(projects.domain.yieldSupply)
    implementation(projects.domain.account)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.card)
    implementation(projects.domain.core)
    implementation(projects.domain.feedback.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.marketing.models)
    implementation(projects.domain.yieldSupply.models)
    runtimeOnly(projects.domain.manageTokens)
    runtimeOnly(projects.domain.offramp)

    /* Domain models */
    api(projects.domain.markets.models)

    /* Compose */
    api(deps.compose.coil)
    api(deps.compose.foundation)
    api(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.ui.utils)
    implementation(deps.lifecycle.compose)
    implementation(deps.androidx.activity.compose)
    implementation(deps.markdown.composeview)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /* Other */
    api(deps.decompose.ext.compose)
    api(deps.kotlin.coroutines)
    api(deps.kotlin.immutable.collections)
    api(deps.kotlin.serialization.core)
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)
    implementation(deps.arrow.core)
    implementation(deps.haze)
    implementation(deps.jodatime)

    /* Core */
    api(projects.core.analytics)
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.ui)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)
    implementation(projects.core.datasource)
    implementation(projects.core.pagination)

    /* Common */
    api(projects.common.routing)
    api(projects.common.ui)
    api(projects.common.uiMarkets)
    implementation(projects.common)
    implementation(projects.common.uiCharts)

    /* Libs */
    api(projects.libs.blockchainSdk)
    implementation(projects.libs.crypto)

    /** Tangem libraries */
    implementation(tangemDeps.blockchain)

    /** Tests */
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.coroutine)
    testImplementation(projects.domain.wallets.models)
}