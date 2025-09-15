plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.swap.presentation"
}

dependencies {
    /** Core modules */
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.configToggles)
    implementation(projects.core.navigation)
    implementation(projects.core.utils)
    implementation(projects.core.ui)
    implementation(projects.common.routing)
    implementation(projects.common.ui)
    implementation(projects.core.decompose) // For Route supertype

    /** Domain modules **/
    implementation(projects.domain.models)
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.balanceHiding)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.transaction)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.settings)
    implementation(projects.domain.staking)
    implementation(projects.domain.feedback)
    implementation(projects.domain.feedback.models)
    implementation(projects.domain.promo)
    implementation(projects.domain.promo.models)
    implementation(projects.domain.txhistory)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.express.models)

    /** Feature modules */
    implementation(projects.features.swap.domain)
    implementation(projects.features.swap.domain.api)
    implementation(projects.features.swap.domain.models)
    implementation(projects.features.wallet.api)
    implementation(projects.features.swap.api)

    /** AndroidX */
    implementation(deps.androidx.activity.compose)
    implementation(deps.androidx.appCompat)
    implementation(deps.androidx.fragment.ktx)
    implementation(deps.lifecycle.viewModel.ktx)
    implementation(deps.androidx.browser)

    /** Compose */
    implementation(deps.arrow.core)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.coil)
    implementation(deps.compose.constraintLayout)

    /** Api */
    implementation(projects.features.swap.api)
    implementation(projects.features.tokendetails.api)

    /** Libs */
    implementation(projects.libs.crypto)

    /** Other libraries */
    implementation(deps.compose.shimmer)
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.kotlin.serialization)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.timber)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}