plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.virtualaccount.onboarding.impl"
}

dependencies {
    /** Api */
    implementation(projects.features.virtualAccounts.onboarding.api)

    /** Impl */
    implementation(projects.features.payment.impl)

    /** Core modules */
    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.utils)
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /** Domain modules */
    implementation(projects.domain.promo)
    implementation(projects.domain.promo.models)

    /** AndroidX */
    implementation(deps.androidx.activity.compose)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling.preview)
    debugImplementation(deps.compose.ui.tooling)

    implementation(deps.decompose.ext.compose)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}