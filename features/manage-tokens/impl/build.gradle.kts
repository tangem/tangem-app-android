plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.managetokens.impl"
}

dependencies {
    /* Tangem libraries */
    implementation(tangemDeps.card.core)

    /* Project - API */
    api(projects.features.commonFeatures.api)
    api(projects.features.manageTokens.api)
    api(projects.features.swapV2.api)

    /* Project - Core */
    api(projects.core.analytics)
    api(projects.core.decompose)
    api(projects.core.ui)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)
    implementation(projects.core.pagination)
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /* Project - Domain */
    api(projects.domain.account)
    api(projects.domain.account.status)
    api(projects.domain.dynamicAddresses)
    api(projects.domain.manageTokens)
    api(projects.domain.manageTokens.models)
    api(projects.domain.models)
    api(projects.domain.notifications)
    api(projects.domain.wallets)
    implementation(projects.domain.core)
    implementation(projects.domain.markets.models)
    implementation(projects.domain.swap.models)
    runtimeOnly(projects.domain.card)
    runtimeOnly(projects.domain.tokens)

    // region Project - Libs
    implementation(projects.libs.blockchainSdk)
    implementation(projects.libs.crypto)
    // endregion

    // region Tangem SDKs
    implementation(tangemDeps.blockchain)
    // endregion

    /* AndroidX */
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)
    implementation(deps.lifecycle.compose)

    /* Compose */
    api(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.material3)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /* Other */
    api(deps.kotlin.coroutines)
    implementation(deps.arrow.core)
    implementation(deps.decompose.ext.compose)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization.core)
}