plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.tokendetails.impl"
}


dependencies {
    /** AndroidX */
    implementation(deps.androidx.activity.compose)

    /** Compose */
    implementation(deps.compose.material)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.navigation)
    implementation(deps.compose.navigation.hilt)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.compose.coil)

    implementation(deps.arrow.core)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.tangem.blockchain)
    implementation(deps.tangem.card.core)
    implementation(deps.timber)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Core modules */
    implementation(projects.common)
    implementation(projects.core.featuretoggles)
    implementation(projects.core.navigation)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** Domain modules */
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.legacy)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.txhistory)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)

    /** Feature Apis */
    implementation(projects.features.tokendetails.api)
}