plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.kyc.impl"
}

dependencies {
    /** Api */
    api(projects.features.kyc.api)

    /** Domain */
    api(projects.domain.visa)
    api(projects.domain.visa.models)
    implementation(projects.domain.models)

    /** Core modules */
    api(projects.core.analytics)
    api(projects.core.decompose)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)
    implementation(projects.core.ui)

    /** Compose libraries */
    api(deps.compose.animation)
    api(deps.decompose.ext.compose)
    implementation(deps.compose.material3)
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)

    /** Other libraries */
    api(deps.androidx.appCompat)
    api(deps.kotlin.coroutines)
    implementation(deps.androidx.core)
    implementation(deps.firebase.crashlytics)
    implementation(deps.sumsub.sdk)
    implementation(deps.arrow.core)
    runtimeOnly(deps.lottie)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}