plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.collectibles.impl"
}

dependencies {
    /** Project - API */
    implementation(projects.features.collectibles.api)

    /** Project - Core */
    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}