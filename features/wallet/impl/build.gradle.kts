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
    packaging {
        resources {
            // To build and run composable preview
            merges += "paymentrequest.proto"
        }
    }
}

dependencies {
    /** AndroidX */
    implementation(deps.androidx.activity.compose)
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)
    implementation(deps.lifecycle.compose)

    /** Compose */
    api(deps.compose.coil)
    api(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.paging)
    implementation(deps.compose.reorderable)
    implementation(deps.compose.reorderableV2)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)

    /** Other libraries */
    api(deps.kotlin.coroutines)
    api(deps.kotlin.immutable.collections)
    api(deps.kotlin.serialization.core)
    implementation(deps.arrow.core)
    implementation(deps.decompose.ext.compose)
    implementation(deps.jodatime)
    implementation(tangemDeps.hot.core)
    api(tangemDeps.card.core)
    implementation(tangemDeps.blockchain)
    implementation(deps.firebase.perf) {
        exclude(group = "com.google.firebase", module = "protolite-well-known-types")
        exclude(group = "com.google.protobuf", module = "protobuf-javalite")
    }
    implementation(deps.haze) {
        exclude(module = "activity-compose")
        exclude(module = "activity")
        exclude(module = "activity-ktx")
    }
    runtimeOnly(deps.material)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Core modules */
    api(projects.core.analytics)
    api(projects.core.analytics.models)
    api(projects.core.configToggles)
    api(projects.core.datasource)
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.ui)
    api(projects.core.utils)
    implementation(projects.core.res)

    implementation(projects.libs.crypto)
    implementation(projects.libs.blockchainSdk)

    /** Domain modules */
    api(projects.domain.account)
    api(projects.domain.account.status)
    api(projects.domain.addressBook)
    api(projects.domain.analytics)
    api(projects.domain.appCurrency)
    api(projects.domain.appTheme)
    api(projects.domain.appUpdate)
    api(projects.domain.assetsdiscovery)
    api(projects.domain.balanceHiding)
    api(projects.domain.card)
    api(projects.domain.common)
    api(projects.domain.demo)
    api(projects.domain.feedback)
    api(projects.domain.hotWallet)
    api(projects.domain.legacy)
    api(projects.domain.models)
    api(projects.domain.networks)
    api(projects.domain.nft)
    api(projects.domain.notifications)
    api(projects.domain.offramp)
    api(projects.domain.onramp)
    api(projects.domain.pushNotificationPreferences)
    api(projects.domain.qrScanning)
    api(projects.domain.quotes)
    api(projects.domain.settings)
    api(projects.domain.staking)
    api(projects.domain.stories)
    api(projects.domain.tokens)
    api(projects.domain.tokens.models)
    api(projects.domain.transaction)
    api(projects.domain.txhistory)
    api(projects.domain.visa)
    api(projects.domain.walletConnect)
    api(projects.domain.walletManager)
    api(projects.domain.wallets)
    api(projects.domain.wallets.models)
    api(projects.domain.yieldSupply)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.appTheme.models)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.core)
    implementation(projects.domain.demo.models)
    implementation(projects.domain.feedback.models)
    implementation(projects.domain.marketing)
    implementation(projects.domain.markets.models)
    implementation(projects.domain.nft.models)
    implementation(projects.domain.onramp.models)
    implementation(projects.domain.qrScanning.models)
    implementation(projects.domain.staking.models)
    implementation(projects.domain.stories.models)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.visa.models)
    implementation(projects.domain.walletConnect.models)
    implementation(projects.domain.yieldSupply.models)

    /** Feature Apis */
    api(projects.features.addressBook.api)
    api(projects.features.biometry.api)
    api(projects.features.commonFeatures.api)
    api(projects.features.feed.api)
    api(projects.features.hotWallet.api)
    api(projects.features.promoBanners.api)
    api(projects.features.polymarket.api)
    api(projects.features.pushNotifications.api)
    api(projects.features.pushNotificationSettings.api)
    api(projects.features.send.api)
    api(projects.features.tangempay.details.api)
    api(projects.features.tangempay.main.api)
    api(projects.features.tokenRecieve.api)
    api(projects.features.virtualAccounts.main.api)
    api(projects.features.wallet.api)
    api(projects.features.walletSettings.api)
    api(projects.features.yieldSupply.api)

    /** Common modules */
    api(projects.common.routing)
    api(projects.common.ui)
    implementation(projects.common)

    /** Test libraries */
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.mockk)
    testImplementation(deps.kotlin.coroutines)
    testImplementation(projects.domain.core)
    testImplementation(projects.common.test)
}