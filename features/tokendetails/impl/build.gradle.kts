plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.tokendetails.impl"
}


dependencies {
    /** AndroidX */
    implementation(deps.androidx.activity.compose)

    /** Compose */
    implementation(deps.compose.material)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.navigation)
    implementation(deps.compose.navigation.hilt)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.compose.coil)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Core modules */
    implementation(projects.core.featuretoggles)
    implementation(projects.core.ui)
    implementation(projects.core.navigation)

    /** Feature Apis */
    implementation(projects.features.tokendetails.api)
}