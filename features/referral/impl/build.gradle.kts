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
    /** Api */
    api(projects.features.referral.api)

    /** Core modules */
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.navigation)
    implementation(projects.core.res)
    implementation(projects.core.utils)
    implementation(projects.core.ui)
    implementation(projects.core.decompose)
    implementation(projects.libs.crypto)
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /** AndroidX */
    implementation(deps.androidx.appCompat)
    implementation(deps.androidx.fragment.ktx)
    implementation(deps.lifecycle.viewModel.ktx)
    implementation(deps.androidx.activity.compose)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui.tooling)

    /** Domain */
    implementation(projects.domain.demo)
    implementation(projects.domain.wallets)
    implementation(projects.domain.legacy)
    implementation(projects.domain.card)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.notifications.models)
    implementation(projects.features.referral.domain)

    /** Other libraries */
    implementation(deps.compose.shimmer)
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.timber)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}