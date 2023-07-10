plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.core.navigation"
}

dependencies {
    implementation(deps.material)
    implementation(deps.reKotlin)
}