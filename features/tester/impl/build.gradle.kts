plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.tester.impl"
}

dependencies {
    /** AndroidX */
    implementation(deps.androidx.activity)
    implementation(deps.androidx.activity.compose)
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.core)
    implementation(deps.lifecycle.compose)
    implementation(deps.lifecycle.runtime.ktx)

    /** Compose */
    api(deps.compose.foundation)
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.compose.material3)
    implementation(deps.compose.navigation)
    implementation(deps.compose.navigation.hilt)
    implementation(deps.compose.reorderable)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.ui.utils)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Domain modules */
    api(projects.domain.account)
    api(projects.domain.common)
    api(projects.domain.feedback)
    api(projects.domain.markets)
    api(projects.domain.settings)
    api(projects.domain.wallets)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.demo.models)
    implementation(projects.domain.feedback.models)
    implementation(projects.domain.manageTokens.models)
    implementation(projects.domain.markets.models)
    implementation(projects.domain.models)
    implementation(projects.domain.offramp)
    implementation(projects.domain.walletManager)
    runtimeOnly(projects.domain.card)
    runtimeOnly(projects.domain.manageTokens)

    /** Data */
    api(projects.data.common)

    /** Other libraries */
    api(deps.kotlin.immutable.collections)
    api(deps.kotlin.serialization)
    implementation(deps.arrow.core)
    implementation(deps.haze)
    implementation(deps.kotlin.coroutines)

    /** Core modules */
    api(projects.core.configToggles)
    api(projects.core.datasource)
    api(projects.core.navigation)
    api(projects.core.ui)
    api(projects.core.utils)
    implementation(projects.core.pagination)

    /** Feature Apis */
    api(projects.features.survey.api)
    api(projects.features.tester.api)
    implementation(projects.features.pushNotifications.api)

    /* SDK */
    implementation(tangemDeps.blockchain)

    /** Other modules */
    api(projects.common.routing)
    api(projects.libs.blockchainSdk)
    implementation(projects.common)
    implementation(projects.common.ui)
    implementation(projects.common.uiCharts)
    implementation(projects.libs.auth)
    implementation(projects.libs.crypto)
    implementation(projects.libs.tangemSdkApi)
}