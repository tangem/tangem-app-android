plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.tangempay.onboarding.impl"
}

dependencies {
    /** Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

    /** Common */
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /** Features api */
    implementation(projects.features.tangempay.onboarding.api)
    implementation(projects.features.tangempay.details.api)
    implementation(projects.features.kyc.api)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}