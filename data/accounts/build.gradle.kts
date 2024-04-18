plugins {
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.accounts"
}

dependencies {

    /* Project - Domain */
    implementation(projects.domain.accounts)

    /* DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
}