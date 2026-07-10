plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.disclaimer.impl"
}

dependencies {
    /* AndroidX */
    implementation(deps.androidx.appCompat)
    implementation(deps.lifecycle.compose)

    /** Compose */
    api(deps.compose.foundation)
    implementation(deps.compose.animation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.accompanist.webView)
    implementation(deps.compose.material3)

    /** Core modules */
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.utils)
    implementation(projects.core.ui)
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /** Domain modules */
    api(projects.domain.card)
    api(projects.domain.notifications)
    api(projects.domain.settings)

    /** Feature modules */
    api(projects.features.disclaimer.api)
    implementation(projects.features.pushNotifications.api)

    /** Other dependencies */
    implementation(deps.decompose)
    implementation(deps.kotlin.coroutines)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}