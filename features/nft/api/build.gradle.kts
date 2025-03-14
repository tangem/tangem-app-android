plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.nft.api"
}

dependencies {

    /* Project - Domain */
    implementation(projects.domain.models)

    /* Project - Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

    /* Compose */
    implementation(deps.compose.runtime)
}