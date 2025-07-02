plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.feeselector.impl"
}

dependencies {
    /** Project - API */
    implementation(projects.features.feeSelector.api)

    /** Project - Core */
    implementation(projects.common.ui)
    implementation(projects.common.routing)
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.pagination)
    implementation(projects.core.navigation)

    /** Project - Domain */
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)

    /** AndroidX */
    implementation(deps.androidx.activity.compose)
    implementation(deps.lifecycle.compose)

    /** Compose */
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.shimmer)

    /*** Tangem SDK */
    implementation(tangemDeps.blockchain)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Other */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.decompose.ext.compose)
    implementation(deps.timber)
}