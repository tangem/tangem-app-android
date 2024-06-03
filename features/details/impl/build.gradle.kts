plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.details.impl"
}

dependencies {

    /* Project - API */
    implementation(projects.features.details.api)
    implementation(projects.features.tester.api)

    /* Project - Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.featuretoggles)
    implementation(projects.core.navigation)
    implementation(projects.core.analytics.models)

    /* Project - Domain */
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.legacy)

    /* AndroidX */
    implementation(deps.androidx.fragment.ktx)
    implementation(deps.androidx.activity.compose)
    implementation(deps.lifecycle.compose)

    /* Compose */
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.shimmer)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /* Other */
    implementation(deps.kotlin.immutable.collections)
}