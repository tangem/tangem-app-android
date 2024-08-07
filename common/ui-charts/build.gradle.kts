plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.common.ui.charts"
}

dependencies {
    /** Project - Core */
    implementation(projects.core.ui)

    /** Compose */
    implementation(deps.tangem.vico.core)
    implementation(deps.tangem.vico.compose)
    implementation(deps.tangem.vico.compose.m3)

    implementation(deps.lifecycle.compose)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.ui.utils)
}