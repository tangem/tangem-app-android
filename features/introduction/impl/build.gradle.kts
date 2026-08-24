plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.introduction.impl"
}

dependencies {
    /** Project - API */
    implementation(projects.features.introduction.api)

    /** Core */
    implementation(projects.common.routing)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.core.navigation)
    implementation(projects.core.res)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** Domain */
    implementation(projects.domain.card)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.lifecycle.compose)

    /** Other */
    implementation(deps.androidx.appCompat)
    implementation(deps.decompose)
    implementation(deps.kotlin.coroutines)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(projects.test.core)
}