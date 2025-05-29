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
    implementation(projects.features.wallet.api)
    implementation(projects.features.walletconnect.api)

    /** Core */
    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /** Domain models */
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.blockaid.models)
    implementation(projects.domain.models)
    implementation(projects.domain.qrScanning.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.walletConnect)
    implementation(projects.domain.walletConnect.models)

    /** Domain */
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.balanceHiding)
    implementation(projects.domain.qrScanning)
    implementation(projects.domain.tokens)
    implementation(projects.domain.wallets)

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

    /** Data */
    implementation(projects.data.card)

    /** Other */
    implementation(deps.arrow.core)
    implementation(deps.decompose.ext.compose)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.timber)
    implementation(tangemDeps.card.core)
}