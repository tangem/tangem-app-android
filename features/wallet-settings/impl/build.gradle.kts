plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.walletsettings.impl"
}

dependencies {

    /* Project - API */
    api(projects.features.hotWallet.api)
    api(projects.features.pushNotificationSettings.api)
    api(projects.features.wallet.api)
    api(projects.features.walletSettings.api)
    implementation(projects.features.pushNotifications.api)

    /* Project - Core */
    api(projects.core.analytics)
    api(projects.core.datasource)
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)
    implementation(projects.core.ui)
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /* Project - Domain */
    api(projects.domain.account)
    api(projects.domain.account.status)
    api(projects.domain.appCurrency)
    api(projects.domain.assetsdiscovery)
    api(projects.domain.balanceHiding)
    api(projects.domain.demo)
    api(projects.domain.nft)
    api(projects.domain.notifications)
    api(projects.domain.settings)
    api(projects.domain.wallets)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.card)
    implementation(projects.domain.common)
    implementation(projects.domain.models)
    implementation(projects.domain.notifications.models)
    implementation(projects.domain.wallets.models)

    /* AndroidX */
    implementation(deps.androidx.appCompat)
    implementation(deps.lifecycle.compose)

    /* Compose */
    api(deps.compose.foundation)
    api(deps.decompose.ext.compose)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.material3)
    implementation(deps.compose.reorderableV2)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /* Other */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization.core)

    /** Tangem libraries */
    implementation(tangemDeps.hot.core)
    implementation(tangemDeps.card.core)
}