plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

dependencies {
    /** Core modules */
    implementation(project(":core:analytics"))
    implementation(projects.core.analytics.models)
    implementation(project(":core:featuretoggles"))
    implementation(project(":core:utils"))
    implementation(project(":core:ui"))

    /** AndroidX */
    implementation(deps.androidx.activity.compose)
    implementation(deps.androidx.appCompat)
    implementation(deps.androidx.fragment.ktx)
    implementation(deps.lifecycle.viewModel.ktx)
    implementation(deps.androidx.browser)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.coil)
    implementation(deps.compose.constraintLayout)

    /** Api */
    implementation(project(":features:swap:api"))

    /** Domain */
    implementation(project(":features:swap:domain"))

    /** Other libraries */
    implementation(deps.compose.shimmer)
    implementation(deps.kotlin.serialization)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.timber)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}
