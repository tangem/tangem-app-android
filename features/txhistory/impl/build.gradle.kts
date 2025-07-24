plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.txhistory.impl"
}

dependencies {
    /* Project - API */
    implementation(projects.features.txhistory.api)

    /* Project - Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.common.routing)
    implementation(projects.core.configToggles)
    implementation(projects.core.analytics)
    implementation(projects.core.pagination)
    implementation(projects.core.navigation)

    /* Project - Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.legacy)
    implementation(projects.domain.card)
    implementation(projects.domain.txhistory)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.balanceHiding)
    implementation(projects.domain.balanceHiding.models)

    /* AndroidX */
    implementation(deps.androidx.activity.compose)
    implementation(deps.lifecycle.compose)

    /* Compose */
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.shimmer)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /* Other */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.decompose.ext.compose)
    implementation(deps.timber)
}