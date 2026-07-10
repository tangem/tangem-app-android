plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.markets.impl"
}

dependencies {
    /* Project - API */
    api(projects.features.markets.api)

    /* Domain */
    api(projects.domain.appCurrency)
    api(projects.domain.markets)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.markets.models)
    implementation(projects.domain.models)

    /* Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.ui.utils)
    implementation(deps.lifecycle.compose)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /* Other */
    implementation(deps.androidx.appCompat)
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.decompose.ext.compose)

    /* Core */
    api(projects.core.decompose)
    api(projects.core.utils)
    implementation(projects.core.ui)

    /* Common */
    implementation(projects.common.ui)
    implementation(projects.common.uiMarkets)
    implementation(projects.common.uiCharts)
    implementation(projects.common.routing)
}