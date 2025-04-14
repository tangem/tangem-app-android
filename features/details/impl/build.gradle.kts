plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.details.impl"
}

dependencies {

    /* Project - API */
    implementation(projects.features.details.api)
    implementation(projects.features.disclaimer.api)
    implementation(projects.features.tester.api)

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
    implementation(projects.domain.models)
    implementation(projects.domain.feedback)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.card)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.walletConnect)
    implementation(projects.domain.balanceHiding)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.legacy)

    /* SDK */
    // TODO: For TangemError model, should be removed after card domain scanning refactoring
    implementation(tangemDeps.card.core)
    // For image resolving
    implementation(tangemDeps.blockchain)

    /* AndroidX */
    implementation(deps.androidx.activity.compose)
    implementation(deps.lifecycle.compose)

    /* Compose */
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.shimmer)
    implementation(deps.compose.coil)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /* Other */
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.reKotlin)
    implementation(deps.timber)
    implementation(deps.arrow.core)
    implementation(deps.arrow.fx)
}