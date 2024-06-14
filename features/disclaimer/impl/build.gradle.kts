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
    /** AndroidX */
    implementation(deps.androidx.fragment.ktx)
    implementation(deps.androidx.appCompat)
    implementation(deps.lifecycle.compose)
    implementation(deps.androidx.activity.compose)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.compose.accompanist.permission)
    implementation(deps.compose.material3)

    /** Core modules */
    implementation(projects.core.ui)
    implementation(projects.core.utils)
    implementation(projects.core.featuretoggles)
    implementation(projects.core.navigation)

    /** Domain modules */
    implementation(projects.domain.models)
    implementation(projects.domain.card)

    /** Feature modules */
    implementation(projects.features.disclaimer.api)

    /** DI */
    implementation(deps.hilt.android)
    implementation(project(":domain:settings"))
    kapt(deps.hilt.kapt)
}