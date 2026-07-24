plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.forceupdate.impl"
}

dependencies {
    /* AndroidX */
    implementation(deps.lifecycle.compose)
    implementation(deps.androidx.activity.compose)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.material3)

    /** Core modules */
    implementation(projects.core.ui)
    implementation(projects.core.utils)
    implementation(projects.core.configToggles)
    implementation(projects.core.navigation)
    implementation(projects.core.decompose)

    /** Feature modules */
    implementation(projects.features.forceUpdate.api)

    /** Domain modules */
    implementation(projects.domain.appUpdate)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}