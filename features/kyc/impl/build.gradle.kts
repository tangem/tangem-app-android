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
    //TODO disable for release because of the permissions
    // implementation(projects.features.kyc.api)

    /** Domain */
    implementation(projects.domain.visa)
    implementation(projects.domain.wallets.models)

    /** Core modules */
    implementation(projects.core.configToggles)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.utils)
    implementation(projects.core.ui)
    implementation(projects.core.res)
    implementation(projects.core.decompose)
    implementation(projects.core.navigation)
    implementation(projects.core.datasource)
    implementation(projects.core.error)

    /** Common */
    implementation(projects.common.ui)
    implementation(projects.common.routing)

    /** Compose libraries */
    implementation(deps.compose.material3)
    implementation(deps.compose.animation)
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.coil)
    implementation(deps.lottie.compose)
    implementation(deps.decompose.ext.compose)
    implementation(deps.androidx.activity.compose)

    /** Other libraries */
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization)
    implementation(deps.timber)
    implementation(deps.firebase.crashlytics)
    implementation(deps.sumsub.sdk)
    implementation(deps.arrow.core)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}