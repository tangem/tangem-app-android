plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.onboarding.v2.api"
}

dependencies {

    /* Project - Domain */
    api(projects.domain.models)

    /* Project - Core */
    api(projects.core.decompose)
    api(projects.core.ui)

    /* Compose */
    implementation(deps.compose.runtime)
}