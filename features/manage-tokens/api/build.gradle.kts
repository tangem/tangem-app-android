plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.managetokens.api"
}

dependencies {
    /* Project - Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.manageTokens.models)

    /* Project - Core */
    implementation(projects.core.ui)
    implementation(projects.core.decompose)

    /* Compose */
    implementation(deps.compose.runtime)
}