plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    id("configuration")
}

android {
    namespace = "com.tangem.features.markets.api"
}

dependencies {
    implementation(deps.compose.foundation)

    /* Project - Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

    /* Project - Domain */
    implementation(projects.domain.core)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.markets.models)
}