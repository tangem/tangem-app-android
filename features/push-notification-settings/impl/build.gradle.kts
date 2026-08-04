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
    api(projects.features.pushNotificationSettings.api)
    api(projects.features.walletSettings.api)
    implementation(projects.features.pushNotifications.api)

    /* Project - Core */
    api(projects.core.analytics)
    api(projects.core.configToggles)
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)
    implementation(projects.core.ui)

    /* Project - Domain */
    api(projects.domain.pushNotificationPreferences)
    implementation(projects.domain.models)
    implementation(projects.domain.notifications)
    implementation(projects.domain.wallets)

    /* AndroidX */
    implementation(deps.androidx.activity)
    implementation(deps.androidx.activity.compose)
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)
    implementation(deps.lifecycle.compose)

    /* Compose */
    api(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.material3)
    implementation(deps.compose.runtime)
    implementation(deps.decompose.ext.compose)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /* Other */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization.core)

    /* Tests */
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.turbine)
}