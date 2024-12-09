plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.tester.impl"
}

dependencies {
    /** AndroidX */
    implementation(deps.androidx.activity.compose)

    /** Compose */
    implementation(deps.compose.material)
    implementation(deps.compose.material3)
    implementation(deps.compose.foundation)
    implementation(deps.compose.navigation)
    implementation(deps.compose.navigation.hilt)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.accompanist.systemUiController)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Domain modules */
    implementation(projects.domain.appTheme)
    implementation(projects.domain.appTheme.models)

    /** Other libraries */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.timber)

    /** Core modules */
    implementation(projects.core.datasource)
    implementation(projects.core.configToggles)
    implementation(projects.core.ui)
    implementation(projects.core.utils)
    implementation(projects.core.navigation)

    /** Feature Apis */
    implementation(projects.features.tester.api)

    /* SDK */
    implementation(deps.tangem.blockchain)

    /** Other modules */
    implementation(projects.common.routing)
    implementation(projects.libs.crypto)
}