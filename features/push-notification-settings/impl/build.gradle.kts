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
    /** Api */
    implementation(projects.features.pushNotificationSettings.api)

    /** Core modules */
    implementation(projects.core.configToggles)

    /** Compose */
    implementation(deps.compose.runtime)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}