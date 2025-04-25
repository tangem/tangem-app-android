plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.walletconnect.impl"
}

dependencies {
    implementation(projects.features.walletconnect.api)
    implementation(projects.domain.walletConnect)
    implementation(projects.domain.walletConnect.models)

    /** Core */
    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /** Domain models */
    implementation(projects.domain.qrScanning.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)

    /** Domain */
    implementation(projects.domain.qrScanning)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** AndroidX */
    implementation(deps.androidx.activity.compose)

    /** Compose */
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.compose.coil)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)

    /** Other */
    implementation(deps.arrow.core)
    implementation(deps.decompose.ext.compose)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.timber)
}