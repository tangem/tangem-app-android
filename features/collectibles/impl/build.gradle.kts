plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.collectibles.impl"
}

dependencies {
    /** Project - API */
    implementation(projects.features.collectibles.api)

    /** Project - Common */
    implementation(projects.common.ui)

    /** Project - Core */
    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.core.navigation)
    implementation(projects.core.res)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.runtime)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)

    /** Other */
    implementation(deps.androidx.appCompat)
    implementation(deps.decompose)
    implementation(deps.kotlin.coroutines)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}