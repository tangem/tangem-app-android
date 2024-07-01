plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("configuration")
}

android {
    namespace = "com.tangem.features.markets.api"
}

dependencies {
    implementation(deps.compose.foundation)

    /* Project - Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
}