plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    id("configuration")
}

android {
    namespace = "com.tangem.features.referral.api"
}

dependencies {
    /* Project - Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

    /* Project - Domain */
    api(projects.domain.models)

    /* Compose */
    implementation(deps.compose.runtime)
}