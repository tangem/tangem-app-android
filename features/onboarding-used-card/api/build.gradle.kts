plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.onboarding.usedcard.api"
}

dependencies {

    /* Project - Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

    /* Project - Domain */
    implementation(projects.domain.models)

    /* Compose */
    implementation(deps.compose.runtime)
}