plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.core.haptic"
}

dependencies {
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}