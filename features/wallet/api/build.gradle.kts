plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

dependencies {
    /** AndroidX */
    implementation(deps.androidx.fragment.ktx)
}