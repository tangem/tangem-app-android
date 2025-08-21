plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.tokendetails.impl"
}


dependencies {
    /** AndroidX */
    implementation(deps.androidx.activity.compose)
    implementation(deps.androidx.paging.runtime)
    implementation(deps.androidx.fragment.ktx)

    /** Compose */
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.compose.coil)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.navigation)
    implementation(deps.compose.navigation.hilt)
    implementation(deps.compose.paging)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.ui.utils)
    implementation(deps.compose.constraintLayout)

    implementation(deps.arrow.core)
    implementation(deps.jodatime)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.reKotlin)
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)
    implementation(deps.timber)
    implementation(deps.lifecycle.compose)
    implementation(deps.kotlin.serialization)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Core modules */
    implementation(projects.common.routing)
    implementation(projects.core.navigation)
    implementation(projects.core.ui)
    implementation(projects.core.utils)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.datasource)
    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.common.ui)

    implementation(projects.libs.crypto)

    /** Domain modules */
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.card)
    implementation(projects.domain.demo)
    implementation(projects.domain.legacy)
    implementation(projects.libs.blockchainSdk)
    implementation(projects.domain.models)
    implementation(projects.domain.settings)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.txhistory)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.balanceHiding)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.transaction)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.staking)
    implementation(projects.domain.markets.models)
    implementation(projects.domain.onramp)
    implementation(projects.domain.onramp.models)
    implementation(projects.domain.promo)
    implementation(projects.domain.promo.models)
    implementation(projects.domain.quotes)
    implementation(projects.domain.notifications.models)

    /** Temp dependency to swap domain */
    implementation(projects.features.swap.domain)
    implementation(projects.features.swap.domain.api)
    implementation(projects.features.swap.domain.models)

    /** Feature Apis */
    implementation(projects.features.tokendetails.api)
    implementation(projects.features.wallet.api)
    implementation(projects.features.staking.api)
    implementation(projects.features.markets.api)
    implementation(projects.features.onramp.api)
    implementation(projects.features.pushNotifications.api)
    implementation(projects.features.swap.api)
    implementation(projects.features.txhistory.api)
    implementation(projects.features.sendV2.api)
    implementation(projects.features.tokenRecieve.api)

    implementation(deps.decompose.ext.compose)

}