plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.polymarket.impl"
}

dependencies {

    /** Feature */
    implementation(projects.features.polymarket.api)

    /** Core */
    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

    /** Domain */
    implementation(projects.domain.models)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.runtime)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)

    /** Decompose */
    implementation(deps.decompose)
    implementation(deps.decompose.ext.compose)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}