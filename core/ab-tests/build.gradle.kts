plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.core.abtests"
}

dependencies {
    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Other libraries */
    implementation(deps.timber)

    /** Core modules */
    implementation(projects.core.analytics.models)
    implementation(projects.core.datasource)
    implementation(projects.core.utils)

    implementation(projects.domain.models)

    /** Amplitude experiment */
    implementation(deps.amplitude.experiment)
}