plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.managetokens.impl"
}

dependencies {
    /** AndroidX */
    implementation(deps.androidx.activity.compose)
    implementation(deps.material)

    /** Compose */
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.compose.coil)
    implementation(deps.compose.constraintLayout)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material)
    implementation(deps.compose.material3)
    implementation(deps.compose.navigation)
    implementation(deps.compose.navigation.hilt)
    implementation(deps.compose.paging)
    implementation(deps.compose.reorderable)
    implementation(deps.compose.shimmer)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)

    /** Other libraries */
    implementation(deps.arrow.core)
    implementation(deps.jodatime)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.tangem.card.core)
    implementation(deps.timber)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Core modules */
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.featuretoggles)
    implementation(projects.core.navigation)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** Project - Data */
    implementation(projects.data.tokens)

    /** Domain modules */
    implementation(projects.domain.card)
    implementation(projects.domain.demo)
    implementation(projects.domain.legacy)
    implementation(projects.libs.blockchainSdk)
    implementation(projects.domain.models)
    implementation(projects.domain.settings)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.appCurrency.models)

    /** Feature Apis */
    implementation(projects.features.manageTokens.api)
}