plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.qrscanning"
}

dependencies {

    /** Domain */
    implementation(projects.domain.qrScanning)
    implementation(projects.domain.qrScanning.models)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}