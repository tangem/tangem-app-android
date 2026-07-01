plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.pushnotificationsettings.impl"
}

dependencies {

    /* Project - API */
    implementation(projects.features.pushNotificationSettings.api)
    implementation(projects.features.pushNotifications.api)
    implementation(projects.features.walletSettings.api)

    /* Project - Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.configToggles)
    implementation(projects.core.navigation)
    implementation(projects.core.analytics)
    implementation(projects.core.utils)

    /* Project - Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.account)
    implementation(projects.domain.pushNotificationPreferences)

    /* AndroidX */
    implementation(deps.androidx.activity.compose)
    implementation(deps.lifecycle.compose)

    /* Compose */
    implementation(deps.compose.ui)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.runtime)
    implementation(deps.compose.shimmer)
    implementation(deps.decompose.ext.compose)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /* Other */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.immutable.collections)

    /* Tests */
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.turbine)
}