plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.referral.presentation"
}

dependencies {
    /** Core modules */
    implementation(project(":core:analytics"))
    implementation(projects.core.analytics.models)
    implementation(project(":core:res"))
    implementation(project(":core:utils"))
    implementation(project(":core:ui"))
    implementation(project(":libs:crypto"))

    /** AndroidX */
    implementation(deps.androidx.appCompat)
    implementation(deps.androidx.fragment.ktx)
    implementation(deps.lifecycle.viewModel.ktx)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui.tooling)

    /** Domain */
    implementation(project(":features:referral:domain"))

    /** Other libraries */
    implementation(deps.compose.shimmer)
    implementation(deps.compose.accompanist.webView)
    implementation(deps.compose.accompanist.systemUiController)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}