plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.managetokens.api"
}

dependencies {
    implementation(deps.compose.foundation)
}