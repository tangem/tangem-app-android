plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("configuration")
}

android {
    namespace = "com.tangem.features.swap.api"
}

dependencies {
    /** Project - Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

    /** Project - Domain */
    api(projects.domain.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)
}