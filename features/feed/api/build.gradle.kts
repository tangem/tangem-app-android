plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    id("configuration")
}

android {
    namespace = "com.tangem.features.feed.api"
}

dependencies {

    /* Kotlin */
    api(deps.kotlin.coroutines)
    api(deps.kotlin.immutable.collections)
    api(deps.kotlin.serialization.core)

    /* Compose */
    api(deps.decompose)
    api(deps.compose.foundation)
    implementation(deps.compose.material3)

    /* Project - Core */
    api(projects.core.analytics.models)
    api(projects.core.decompose)
    api(projects.core.ui)

    /* Project - Domain */
    api(projects.domain.appCurrency.models)
    api(projects.domain.markets.models)
    api(projects.domain.models)
    api(projects.domain.news)
}