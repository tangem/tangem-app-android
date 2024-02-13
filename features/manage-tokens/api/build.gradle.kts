plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.managetokens.api"
}

dependencies {
    /** AndroidX */
    implementation(deps.androidx.fragment.ktx)

    implementation(deps.compose.foundation)
    implementation(deps.compose.navigation)
}