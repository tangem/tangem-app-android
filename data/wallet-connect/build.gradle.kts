plugins {
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.android.library)
    id("configuration")
}

android {
    namespace = "com.tangem.data.walletconnect"
}

dependencies {

    /* Project - Domain */
    implementation(projects.domain.walletConnect)
    implementation(projects.domain.wallets.models)

    /* Project - Data */
    implementation(projects.core.datasource)

    /* Project - Core */
    implementation(projects.core.utils)

    /* DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)

    /* Other */
    implementation(deps.kotlin.coroutines)
}