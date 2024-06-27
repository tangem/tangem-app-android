plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("configuration")
}

android {
    namespace = "com.tangem.features.staking.api"
}

dependencies {

    /** AndroidX */
    implementation(deps.androidx.fragment.ktx)
}