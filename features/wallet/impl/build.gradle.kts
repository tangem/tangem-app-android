plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

dependencies {
    /** AndroidX */
    implementation(deps.androidx.activity.compose)
    implementation(deps.material)

    /** Compose */
    implementation(deps.compose.coil)
    implementation(deps.compose.constraintLayout)
    implementation(deps.compose.material)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.navigation)
    implementation(deps.compose.navigation.hilt)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.shimmer)
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.compose.reorderable)

    /** Other libraries */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.tangem.card.core)
    implementation(deps.tangem.blockchain)
    implementation(deps.arrow.core)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Core modules */
    implementation(projects.core.featuretoggles)
    implementation(projects.core.navigation)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** Domain modules */
    implementation(projects.common)
    implementation(projects.domain.card)
    implementation(projects.domain.demo)
    implementation(projects.domain.legacy)
    implementation(projects.domain.models)
    implementation(projects.domain.settings)
    implementation(projects.domain.tokens)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)

    /** Feature Apis */
    implementation(projects.features.wallet.api)
}