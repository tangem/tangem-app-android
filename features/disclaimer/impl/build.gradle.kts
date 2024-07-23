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
    implementation(deps.lifecycle.compose)
    implementation(deps.androidx.activity.compose)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.compose.accompanist.permission)
    implementation(deps.compose.accompanist.webView)
    implementation(deps.compose.material3)
    implementation(deps.compose.material)

    /** Core modules */
    implementation(projects.core.ui)
    implementation(projects.core.utils)
    implementation(projects.core.featuretoggles)
    implementation(projects.core.navigation)
    implementation(projects.core.decompose)
    implementation(projects.common.routing)

    /** Domain modules */
    implementation(projects.domain.models)
    implementation(projects.domain.card)
    implementation(projects.domain.settings)

    /** Feature modules */
    implementation(projects.features.disclaimer.api)
    implementation(projects.features.pushNotifications.api)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}