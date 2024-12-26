plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.stories.impl"
}

dependencies {

    implementation(deps.compose.ui)
    implementation(deps.compose.material3)

    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

    /** Feature modules */
    implementation(projects.features.stories.api)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}