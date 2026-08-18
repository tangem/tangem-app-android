plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.kyc.impl"
}

dependencies {
    /** Api */
    api(projects.features.kyc.api)

    api(projects.core.decompose)
    implementation(deps.compose.ui)

    /** DI */
    implementation(deps.hilt.android)
    implementation(deps.androidx.appCompat)
    implementation(deps.decompose)
    implementation(deps.kotlin.coroutines)
    implementation(projects.core.utils)
    kapt(deps.hilt.kapt)
}