plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("configuration")
}

android {
    namespace = "com.tangem.features.tokendetails.api"
}

dependencies {
    implementation(projects.domain.tokens.models)

    /** AndroidX */
    implementation(deps.androidx.fragment.ktx)
}