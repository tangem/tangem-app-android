plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.feed.earn.impl"
}

dependencies {
    /* Project - API */
    implementation(projects.features.feed.api)

    /* Project - Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /* Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.ui.utils)

    /* Other */
    implementation(deps.androidx.appCompat)
    implementation(deps.decompose)
    implementation(deps.decompose.ext.compose)
    implementation(deps.kotlin.coroutines)
    runtimeOnly(deps.room.runtime)

    /* DI */
    implementation(deps.hilt.android)
}