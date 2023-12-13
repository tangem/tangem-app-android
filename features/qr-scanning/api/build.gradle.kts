plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.qr_scanning.api"
}

dependencies {

    /** AndroidX */
    implementation(deps.androidx.fragment.ktx)
}
