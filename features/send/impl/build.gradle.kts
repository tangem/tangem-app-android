plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.send.impl"
}

dependencies {
    /** AndroidX */
    implementation(deps.androidx.fragment.ktx)
    implementation(deps.androidx.appCompat)
    implementation(deps.androidx.paging.runtime)

    /** Other dependencies */
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.material)
    implementation(deps.arrow.core)
    implementation(deps.lifecycle.compose)
    implementation(deps.jodatime)
    implementation(deps.timber)
    implementation(deps.reKotlin)
    implementation(deps.kotlin.serialization)

    /** Compose */
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.compose.material3)
    implementation(deps.compose.material)
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.navigation)
    implementation(deps.compose.navigation.hilt)
    implementation(deps.compose.paging)
    implementation(deps.compose.constraintLayout)

    /** Tangem SDKs */
    implementation(tangemDeps.card.core)
    implementation(tangemDeps.blockchain)

    /** Core modules */
    implementation(projects.core.configToggles)
    implementation(projects.core.ui)
    implementation(projects.core.utils)
    implementation(projects.core.navigation)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.datasource)
    implementation(projects.core.decompose)

    /** Common */
    implementation(projects.common.ui)
    implementation(projects.common.routing)

    /** Libs */
    implementation(projects.libs.crypto)

    /** Domain modules */
    implementation(projects.domain.models)
    implementation(projects.domain.legacy)
    implementation(projects.libs.blockchainSdk)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.txhistory)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.transaction)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.card)
    implementation(projects.domain.balanceHiding)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.feedback)
    implementation(projects.domain.qrScanning)
    implementation(projects.domain.qrScanning.models)
    implementation(projects.domain.settings)

    /** Feature modules */
    implementation(projects.features.send.api)
    implementation(projects.features.tokendetails.api)
    implementation(projects.features.qrScanning.api)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}