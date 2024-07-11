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

    /* Compose */
    implementation(deps.compose.coil)
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
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.timber)

    /* Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.featuretoggles)

    implementation(projects.common.ui)
    implementation(projects.common.uiCharts)
}