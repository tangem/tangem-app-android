plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.account.api"
}

dependencies {

    /* Project - Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.common.ui)

    /* Project - Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.core)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
}