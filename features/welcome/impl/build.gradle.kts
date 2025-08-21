plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.welcome.impl"
}

dependencies {
    implementation(projects.features.welcome.api)
    implementation(projects.features.wallet.api)

    /** Core */
    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.core.navigation)
    implementation(projects.core.ui)
    implementation(projects.core.analytics)
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /** Domain models */
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)

    /** Domain */
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.wallets)
    implementation(projects.domain.card)
    implementation(projects.domain.settings)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** AndroidX */
    implementation(deps.androidx.activity.compose)

    /** Compose */
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.compose.coil)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)

    /** Other */
    implementation(deps.arrow.core)
    implementation(deps.decompose)
    implementation(deps.decompose.ext.compose)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.timber)
    implementation(tangemDeps.card.core)
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.hot.core)
}