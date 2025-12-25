plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.news.list.api"
}

dependencies {
    implementation(deps.compose.foundation)

    /* Project - Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

    /* Compose */
    implementation(deps.compose.runtime)
}