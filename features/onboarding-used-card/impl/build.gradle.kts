plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.onboarding.usedcard.impl"
}

dependencies {
    /** Api */
    implementation(projects.features.onboardingUsedCard.api)
    implementation(projects.features.biometry.api)
    implementation(projects.features.pushNotifications.api)

    /** Core modules */
    implementation(projects.core.analytics)
    implementation(projects.core.configToggles)
    implementation(projects.core.utils)
    implementation(projects.core.ui)
    implementation(projects.core.res)
    implementation(projects.core.decompose)
    implementation(projects.core.navigation)

    /** Domain */
    implementation(projects.domain.settings)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.models)

    /** Common */
    implementation(projects.common.routing)

    /** AndroidX libraries */
    implementation(deps.androidx.core.ktx)
    implementation(deps.lifecycle.runtime.ktx)

    /** Compose libraries */
    implementation(deps.compose.material3)
    implementation(deps.compose.animation)
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.decompose.ext.compose)
    implementation(deps.androidx.activity.compose)

    /** Other libraries */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.timber)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}