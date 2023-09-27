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
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.featuretoggles)
    implementation(projects.core.utils)
    implementation(projects.core.ui)
    implementation(projects.common)

    /** Domain modules **/
    implementation(projects.domain.balanceHiding)

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
    implementation(deps.compose.accompanist.systemUiController)

    /** Api */
    implementation(projects.features.swap.api)

    /** Domain */
    implementation(projects.features.swap.domain)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.settings)

    /** Other libraries */
    implementation(deps.compose.shimmer)
    implementation(deps.kotlin.serialization)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.timber)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}
