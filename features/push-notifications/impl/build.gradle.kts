plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.pushnotifications.impl"
}

dependencies {
    /** AndroidX */
    implementation(deps.androidx.fragment.ktx)
    implementation(deps.androidx.activity.compose)

    /** Compose */
    implementation(deps.compose.material3)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.foundation)
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.compose.accompanist.permission)

    /** Other dependencies */
    implementation(deps.timber)
    implementation(deps.arrow.core)
    implementation(deps.kotlin.immutable.collections)

    /** Core modules */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.configToggles)
    implementation(projects.core.navigation)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)

    /** Common modules */
    implementation(projects.common.routing)

    /** Domain module */
    implementation(projects.domain.settings)
    implementation(projects.domain.notifications.toggles)
    implementation(projects.domain.notifications)

    /** Feature modules */
    implementation(projects.features.pushNotifications.api)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}