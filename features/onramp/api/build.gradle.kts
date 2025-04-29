plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("configuration")
}

android {
    namespace = "com.tangem.features.onramp.api"
}

dependencies {
    /* Project - Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.deepLinks)

    /* Project - Domain */
    implementation(projects.domain.onramp.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)

    /* Compose */
    implementation(deps.compose.runtime)
}