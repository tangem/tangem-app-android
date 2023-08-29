plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.wallet"
}

dependencies {
    implementation(projects.core.utils)
    implementation(projects.data.source.preferences)
    implementation(projects.domain.wallets)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}