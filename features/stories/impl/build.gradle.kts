plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.stories.impl"
}

dependencies {
    /** Feature modules */
    implementation(projects.features.stories.api)
    /** Domain modules */
    implementation(projects.domain.promo)
    implementation(projects.domain.promo.models)

    /** Project - Common */
    implementation(projects.common.routing)

    /** Project - Core */
    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.core.navigation)
    implementation(projects.core.res)
    implementation(projects.core.ui)
    implementation(projects.core.analytics)

    /** AndroidX */
    implementation(deps.androidx.activity.compose)
    implementation(deps.lifecycle.compose)

    /** Compose */
    implementation(deps.compose.material3)
    implementation(deps.compose.ui.tooling)

    /** Others */
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.compose.coil)
    implementation(deps.timber)
    implementation(deps.arrow.core)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}