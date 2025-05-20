plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.qrscanning"
}

dependencies {

    /** Domain */
    api(projects.domain.models)
    implementation(projects.domain.qrScanning.models)
    implementation(projects.domain.tokens.models)

    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)
}