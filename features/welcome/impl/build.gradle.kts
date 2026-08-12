plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.welcome.impl"
}

dependencies {
    implementation(projects.features.welcome.api)
    implementation(projects.features.wallet.api)
    implementation(projects.features.onboardingV2.api)

    /** Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.analytics)
    implementation(projects.core.utils)
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /** Domain models */
    implementation(projects.domain.models)

    /** Domain */
    implementation(projects.domain.wallets)
    implementation(projects.domain.card)
    implementation(projects.domain.settings)
    implementation(projects.core.analytics.models)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** AndroidX */

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)

    /** Other */
    implementation(deps.arrow.core)
    implementation(deps.decompose)
    implementation(deps.decompose.ext.compose)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.androidx.appCompat)
    implementation(deps.kotlin.coroutines)
    implementation(deps.lifecycle.compose)
    implementation(deps.lifecycle.runtime.ktx)
    implementation(tangemDeps.card.core)
    api(projects.domain.common)
}