plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("configuration")
}

android {
    namespace = "com.tangem.features.send.api"
}

dependencies {
    /** Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

    /** Domain models */
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.tokens.models)

    /** AndroidX */
    implementation(deps.androidx.fragment.ktx)
}