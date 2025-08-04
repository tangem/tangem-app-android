plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.tester.impl"
}

dependencies {
    /** AndroidX */
    implementation(deps.androidx.activity.compose)

    /** Compose */
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material)
    implementation(deps.compose.material3)
    implementation(deps.compose.navigation)
    implementation(deps.compose.navigation.hilt)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.ui.utils)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Domain modules */
    implementation(projects.domain.appTheme)
    implementation(projects.domain.appTheme.models)
    implementation(projects.domain.card)
    implementation(projects.domain.feedback)
    implementation(projects.domain.markets.models)
    implementation(projects.domain.markets)
    implementation(projects.domain.manageTokens.models)
    implementation(projects.domain.manageTokens)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.wallets)

    /** Other libraries */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.timber)

    /** Core modules */
    implementation(projects.core.datasource)
    implementation(projects.core.configToggles)
    implementation(projects.core.ui)
    implementation(projects.core.utils)
    implementation(projects.core.navigation)
    implementation(projects.core.pagination)

    /** Feature Apis */
    implementation(projects.features.tester.api)
    implementation(projects.features.pushNotifications.api)

    /* SDK */
    implementation(tangemDeps.blockchain)

    /** Other modules */
    implementation(projects.common.routing)
    implementation(projects.libs.blockchainSdk)
    implementation(projects.libs.crypto)
}