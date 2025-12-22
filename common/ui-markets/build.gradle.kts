plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.common.ui.markets"
}

dependencies {
    /** Project - Core */
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** Project - Common */
    implementation(projects.common.uiCharts)
    implementation(projects.common.ui)

    /** Project - Domain */
    implementation(projects.domain.models)

    implementation(deps.lifecycle.compose)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.ui.utils)
    implementation(deps.kotlin.immutable.collections)
}