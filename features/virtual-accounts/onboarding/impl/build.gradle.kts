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
    /** Core */
    implementation(projects.core.decompose)
    implementation(projects.core.error)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** Common */
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /** Api */
    implementation(projects.features.virtualAccounts.onboarding.api)

    /** Domain */
    implementation(projects.domain.common)
    implementation(projects.domain.models)
    implementation(projects.domain.visa)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.decompose.ext.compose)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Other */
    implementation(deps.arrow.core)
}