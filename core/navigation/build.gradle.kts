plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.core.navigation"
}

dependencies {
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    implementation(deps.material)
    implementation(deps.reKotlin)
    implementation(deps.timber)
}