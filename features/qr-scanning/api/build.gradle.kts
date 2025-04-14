plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.qrscanning.api"
}

dependencies {
    /** Domain models */
    implementation(projects.domain.qrScanning.models)

    /** Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

    /** AndroidX */
    implementation(deps.androidx.fragment.ktx)
}