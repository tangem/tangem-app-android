plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.virtualaccount.main.api"
}

dependencies {
    /** Core */
    api(projects.core.decompose)
    api(projects.core.ui)

    /** Compose */
    api(deps.compose.foundation)
    implementation(deps.compose.runtime)
    implementation(deps.compose.ui)
}