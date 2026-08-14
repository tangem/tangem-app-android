plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
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
    implementation(projects.domain.markets)
    implementation(projects.domain.markets.models)
    implementation(projects.domain.models)

    /* Project - Core */
    implementation(projects.core.analytics.models)
    implementation(projects.core.decompose)
    implementation(projects.core.pagination)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /* Project - Common */
    implementation(projects.common.uiCharts)

    /* Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.material3)
    implementation(deps.lifecycle.compose)

    /* Other */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}