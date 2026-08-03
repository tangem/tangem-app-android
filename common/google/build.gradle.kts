plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.common.google"
}

dependencies {
    implementation(projects.core.utils)

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.androidx.activity)

    implementation(deps.googlePlay.services.wallet)
    implementation(deps.googlePlay.services.auth)
}