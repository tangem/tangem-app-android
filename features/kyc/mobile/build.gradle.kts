plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.kyc.mobile"
}

dependencies {
    /** Api */
    implementation(projects.features.kyc.api)

    /** Domain */
    implementation(projects.domain.visa)
    implementation(projects.domain.wallets.models)

    /** Core modules */
    implementation(projects.core.ui)
    implementation(projects.core.res)
    implementation(projects.core.decompose)
    implementation(projects.core.navigation)
    implementation(projects.core.error)

    /** Common */
    implementation(projects.common.ui)

    /** Compose libraries */
    implementation(deps.compose.material3)
    implementation(deps.compose.animation)
    implementation(deps.compose.ui)

    /** Other libraries */
    implementation(deps.timber)
    implementation(deps.sumsub.sdk)
    implementation(deps.arrow.core)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}