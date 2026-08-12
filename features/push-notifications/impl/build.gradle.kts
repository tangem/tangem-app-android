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
    implementation(deps.androidx.activity.compose)

    /** Compose */
    implementation(deps.compose.material3)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.foundation)
    implementation(deps.lifecycle.compose)

    /** Other dependencies */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.immutable.collections)

    /** Core modules */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.utils)
    implementation(projects.core.abTests)

    /** Common modules */
    implementation(projects.common.routing)

    /** Domain module */
    implementation(projects.domain.settings)
    implementation(projects.domain.notifications)
    implementation(projects.domain.pushNotificationPreferences)
    implementation(projects.domain.common)
    implementation(projects.domain.models)
    implementation(projects.domain.wallets)

    /** Feature modules */
    implementation(projects.features.pushNotifications.api)

    /** DI */
    implementation(deps.hilt.android)
    implementation(deps.androidx.appCompat)
    implementation(deps.decompose)
    implementation(deps.kotlin.coroutines)
    kapt(deps.hilt.kapt)

    /** Test */
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.kotlin.coroutines)
}