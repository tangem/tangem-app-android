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
    implementation(projects.domain.stories)
    implementation(projects.domain.stories.models)

    /** Project - Common */
    implementation(projects.common.routing)

    /** Project - Core */
    implementation(projects.core.decompose)
    implementation(projects.core.res)
    implementation(projects.core.ui)
    implementation(projects.core.utils)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)

    /** AndroidX */
    implementation(deps.androidx.activity.compose)
    implementation(deps.lifecycle.compose)

    /** Compose */
    implementation(deps.compose.material3)
    implementation(deps.compose.ui.tooling)

    /** Others */
    implementation(deps.arrow.core)
    implementation(deps.compose.coil)
    implementation(deps.kotlin.immutable.collections)

    /** DI */
    implementation(deps.hilt.android)
    implementation(deps.androidx.appCompat)
    implementation(deps.decompose)
    implementation(deps.kotlin.coroutines)
    kapt(deps.hilt.kapt)
}