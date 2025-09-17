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
    implementation(projects.features.walletSettings.api)
    implementation(projects.features.manageTokens.api)
    implementation(projects.features.nft.api)
    implementation(projects.features.onboardingV2.api)
    implementation(projects.features.pushNotifications.api)
    implementation(projects.features.hotWallet.api)
    implementation(projects.features.wallet.api)

    /* Project - Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.configToggles)
    implementation(projects.core.navigation)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /* Project - Domain */
    implementation(projects.domain.legacy)
    implementation(projects.domain.card)
    implementation(projects.domain.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.demo)
    implementation(projects.domain.nft)
    implementation(projects.domain.settings)
    implementation(projects.domain.notifications.models)
    implementation(projects.domain.notifications)

    /* AndroidX */
    implementation(deps.androidx.fragment.ktx)
    implementation(deps.androidx.activity.compose)
    implementation(deps.lifecycle.compose)

    /* Compose */
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.shimmer)
    implementation(deps.decompose.ext.compose)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /* Other */
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.timber)
    implementation(deps.reKotlin)

    /** Tangem libraries */
    implementation(tangemDeps.hot.core)
    implementation(tangemDeps.card.core)
}