plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.tangempay.commonui"
}

dependencies {
    /** Core */
    api(projects.core.ui)

    /** Compose */
    api(deps.compose.foundation)
    implementation(deps.compose.coil)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
}