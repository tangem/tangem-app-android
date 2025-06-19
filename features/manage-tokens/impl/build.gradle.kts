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
    /* Project - API */
    implementation(projects.features.manageTokens.api)
    implementation(projects.features.swapV2.api)

    /* Project - Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.configToggles)
    implementation(projects.core.analytics)
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /* Project - Domain */
    implementation(projects.domain.card)
    implementation(projects.domain.legacy)
    implementation(projects.domain.manageTokens)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)

    /* AndroidX */
    implementation(deps.androidx.activity.compose)
    implementation(deps.lifecycle.compose)

    /* Compose */
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material) // For button colors
    implementation(deps.compose.material3)
    implementation(deps.compose.shimmer)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /* Other */
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.decompose.ext.compose)
    implementation(deps.timber)
    implementation(deps.reKotlin) // need for legacy onboarding
}