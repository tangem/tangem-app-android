plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.feed.crypto.impl"
}

dependencies {
    /* Project - API */
    implementation(projects.features.feed.api)
    implementation(projects.features.feed.crypto.api)
    implementation(projects.features.promoBanners.api)

    /* Project - Domain */
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.balanceHiding)
    implementation(projects.domain.common)
    implementation(projects.domain.feed.search)
    implementation(projects.domain.markets)
    implementation(projects.domain.markets.models)
    implementation(projects.domain.models)
    implementation(projects.domain.search)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets)

    /* Project - Core */
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.decompose)
    implementation(projects.core.navigation)
    implementation(projects.core.pagination)
    implementation(projects.core.remote)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /* Project - Common */
    implementation(projects.common.routing)
    implementation(projects.common.ui)
    implementation(projects.common.uiCharts)
    implementation(projects.common.uiMarkets)

    /* Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui.tooling)
    implementation(deps.lifecycle.compose)
    api(deps.decompose.ext.compose)

    /* Other */
    implementation(deps.androidx.appCompat)
    implementation(deps.arrow.core)
    implementation(deps.decompose)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization.core)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /* Tests */
    testImplementation(projects.test.core)
    testImplementation(projects.common.test)
    testImplementation(tangemDeps.blockchain)
    testImplementation(deps.kotlin.serialization)
}