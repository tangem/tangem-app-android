plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.serialization)
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

    /** Compose */
    implementation(deps.compose.coil)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.ui.utils)

    implementation(deps.arrow.core)
    implementation(deps.jodatime)
    implementation(deps.kotlin.immutable.collections)
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)
    implementation(deps.lifecycle.compose)
    implementation(deps.kotlin.serialization)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Core modules */
    implementation(projects.common.routing)
    implementation(projects.core.navigation)
    implementation(projects.core.res)
    implementation(projects.core.ui)
    implementation(projects.core.utils)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.datasource)
    implementation(projects.core.decompose)
    implementation(projects.common.ui)
    implementation(projects.features.rating.api)

    implementation(projects.libs.blockchainSdk)
    implementation(projects.libs.crypto)

    /** Domain modules */
    implementation(projects.domain.account.status)
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.balanceHiding)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.card)
    implementation(projects.domain.demo)
    implementation(projects.domain.dynamicAddresses)
    implementation(projects.domain.dynamicAddresses.models)
    implementation(projects.domain.feedback)
    implementation(projects.domain.marketing.models)
    implementation(projects.domain.models)
    implementation(projects.domain.notifications.models)
    implementation(projects.domain.offramp)
    implementation(projects.domain.onramp)
    implementation(projects.domain.onramp.models)
    implementation(projects.domain.staking)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.transaction)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.txhistory)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.yieldSupply)
    implementation(projects.domain.yieldSupply.models)

    /** Temp dependency to swap domain */
    implementation(projects.features.swap.domain)

    /** Feature Apis */
    implementation(projects.features.send.api)
    implementation(projects.features.tokendetails.api)
    implementation(projects.features.wallet.api)
    implementation(projects.features.markets.api)
    implementation(projects.features.marketing.api)
    implementation(projects.features.pushNotifications.api)
    implementation(projects.features.txhistory.api)
    implementation(projects.features.tokenRecieve.api)
    implementation(projects.features.yieldSupply.api)
    implementation(projects.features.commonFeatures.api)
    implementation(projects.common)
    implementation(projects.domain.core)
    implementation(projects.domain.staking.models)

    implementation(deps.decompose.ext.compose)
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)
    implementation(deps.androidx.core)
    implementation(deps.haze)

    /** Tests */
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.kotlin.coroutines)
    testImplementation(deps.kotlin.datetime)
    api(deps.kotlin.coroutines)
    api(projects.domain.account)
    api(projects.domain.common)
    api(projects.domain.visa)
    api(projects.domain.walletManager)
    api(projects.features.swap.api)
}