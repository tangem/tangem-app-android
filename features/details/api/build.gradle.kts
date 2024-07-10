plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.details.api"
}

dependencies {

    /* Project - Domain */
    implementation(projects.domain.wallets.models)

    /* Project - Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
}