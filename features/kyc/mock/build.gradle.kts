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
    implementation(projects.features.kyc.api)

    implementation(projects.core.decompose)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}