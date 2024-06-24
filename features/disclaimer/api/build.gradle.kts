plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.disclaimer.api"
}

dependencies {

    /** AndroidX */
    implementation(deps.androidx.fragment.ktx)
}