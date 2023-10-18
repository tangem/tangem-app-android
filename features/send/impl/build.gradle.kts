plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.send.impl"
}

dependencies {
    /** AndroidX */
    implementation(deps.androidx.fragment.ktx)
    implementation(deps.androidx.appCompat)
    implementation(deps.material)

    /** Compose */
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.compose.material3)
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.navigation)

    /** Core modules */
    implementation(projects.core.featuretoggles)
    implementation(projects.core.ui)

    /** Domain modules */
    implementation(projects.domain.tokens.models)

    /** Feature modules */
    implementation(projects.features.send.api)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}