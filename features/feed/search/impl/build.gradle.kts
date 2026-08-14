plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.feed.search.impl"
}

dependencies {
    /* Project - API */
    implementation(projects.features.feed.api)
    implementation(projects.features.feed.search.api)

    /* Project - Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /* Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.material3)
    implementation(deps.lifecycle.compose)

    /* Other */
    implementation(deps.kotlin.coroutines)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}