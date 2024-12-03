plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.markets.impl"
}

dependencies {
    /* Project - API */
    api(projects.features.markets.api)
    implementation(projects.core.navigation)

    /* Domain */
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.balanceHiding)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.card)
    implementation(projects.domain.demo)
    implementation(projects.domain.feedback)
    implementation(projects.domain.manageTokens)
    implementation(projects.domain.markets)
    implementation(projects.domain.onramp.models)
    implementation(projects.domain.staking.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)

    // FIXME [REDACTED_TASK_KEY]
    // Remove the "Buy" and "Sell" actions from the redux middleware.
    // Instead, create some kind of interface for such cases.
    /* Redux -_- */
    implementation(projects.domain.legacy)
    implementation(deps.reKotlin)

    /* Compose */
    implementation(deps.compose.coil)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.ui.utils)
    implementation(deps.lifecycle.compose)
    implementation(deps.androidx.activity.compose)
    implementation(deps.markdown.composeview)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /* Other */
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.timber)
    implementation(deps.decompose.ext.compose)

    /* Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.configToggles)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)

    /* Common */
    implementation(projects.common.ui)
    implementation(projects.common.uiCharts)
    implementation(projects.common.routing)

    /* Libs */
    implementation(projects.libs.crypto)
    implementation(projects.libs.blockchainSdk)
}