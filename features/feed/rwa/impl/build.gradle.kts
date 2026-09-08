plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.feed.rwa.impl"
}

dependencies {
    /* Project - API */
    implementation(projects.features.feed.api)

    /* Project - Domain */
    implementation(projects.domain.markets)
    implementation(projects.domain.markets.models)

    /* Project - Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /* Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.material3)
    implementation(deps.lifecycle.compose)

    /* Other */
    implementation(deps.androidx.appCompat)
    implementation(deps.arrow.core)
    implementation(deps.decompose)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.serialization.core)
    implementation(deps.kotlin.immutable.collections)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /* Tests */
    testImplementation(projects.test.core)
}