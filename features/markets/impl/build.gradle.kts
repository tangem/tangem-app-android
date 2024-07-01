plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.markets.impl"
}

dependencies {
    implementation(deps.compose.coil)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.ui.utils)

    implementation(deps.kotlin.immutable.collections)

    implementation(projects.core.ui)
    implementation(projects.common.ui)
    implementation(projects.common.uiCharts)
}