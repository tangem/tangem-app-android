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
    api(projects.domain.qrScanning.models)

    /** Core */
    api(projects.core.decompose)
    api(projects.core.ui)
}