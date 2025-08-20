plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.wallet.impl"
}

dependencies {
    /** AndroidX */
    implementation(deps.androidx.activity.compose)
    implementation(deps.lifecycle.compose)
    implementation(deps.material)

    /** Compose */
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.compose.coil)
    implementation(deps.compose.constraintLayout)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.paging)
    implementation(deps.compose.reorderable)
    implementation(deps.compose.shimmer)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)

    /** Other libraries */
    implementation(deps.arrow.core)
    implementation(deps.decompose.ext.compose)
    implementation(deps.googlePlay.review)
    implementation(deps.jodatime)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.reKotlin)
    implementation(tangemDeps.card.core)
    implementation(tangemDeps.blockchain)
    implementation(deps.timber)
    implementation(deps.firebase.perf) {
        exclude(group = "com.google.firebase", module = "protolite-well-known-types")
        exclude(group = "com.google.protobuf", module = "protobuf-javalite")
    }

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Core modules */
    implementation(projects.core.configToggles)
    implementation(projects.core.navigation)
    implementation(projects.core.ui)
    implementation(projects.core.utils)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.decompose)
    implementation(projects.core.datasource)
    implementation(projects.core.res)

    implementation(projects.libs.crypto)
    implementation(projects.libs.blockchainSdk)

    /** Domain modules */
    implementation(projects.domain.analytics)
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.balanceHiding)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.card)
    implementation(projects.domain.walletManager)
    implementation(projects.domain.demo)
    implementation(projects.domain.feedback)
    implementation(projects.domain.feedback.models)
    implementation(projects.domain.legacy)
    implementation(projects.domain.markets.models)
    implementation(projects.domain.models)
    implementation(projects.domain.networks)
    implementation(projects.domain.nft)
    implementation(projects.domain.nft.models)
    implementation(projects.domain.onramp)
    implementation(projects.domain.onramp.models)
    implementation(projects.domain.promo)
    implementation(projects.domain.promo.models)
    implementation(projects.domain.quotes)
    implementation(projects.domain.settings)
    implementation(projects.domain.staking)
    implementation(projects.domain.staking.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.txhistory)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.visa)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.notifications)
    implementation(projects.domain.notifications.toggles)

    /** Feature Apis */
    implementation(projects.features.details.api)
    implementation(projects.features.hotWallet.api)
    implementation(projects.features.manageTokens.api)
    implementation(projects.features.markets.api)
    implementation(projects.features.onboardingV2.api)
    implementation(projects.features.onramp.api)
    implementation(projects.features.pushNotifications.api)
    implementation(projects.features.swap.api)
    implementation(projects.features.tester.api)
    implementation(projects.features.tokendetails.api)
    implementation(projects.features.wallet.api)
    implementation(projects.features.walletSettings.api)
    implementation(projects.features.biometry.api)
    implementation(projects.features.nft.api)
    implementation(projects.features.sendV2.api)
    implementation(projects.features.kyc.api)

    /** Common modules */
    implementation(projects.common)
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /** Test libraries */
    implementation(deps.test.junit)
    implementation(deps.test.truth)
}