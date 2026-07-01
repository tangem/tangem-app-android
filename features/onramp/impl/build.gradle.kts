plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.onramp.impl"
}

dependencies {

    /** Project - API */
    api(projects.features.commonFeatures.api)
    api(projects.features.onramp.api)

    /** Project - Core */
    api(projects.core.analytics)
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.ui)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)

    /** Project - Common */
    api(projects.common.routing)
    api(projects.common.ui)
    implementation(projects.common)

    /** Project - Domain */
    api(projects.domain.account)
    api(projects.domain.account.status)
    api(projects.domain.appCurrency)
    api(projects.domain.appTheme)
    api(projects.domain.balanceHiding)
    api(projects.domain.demo)
    api(projects.domain.legacy)
    api(projects.domain.models)
    api(projects.domain.offramp)
    api(projects.domain.onramp)
    api(projects.domain.onramp.models)
    api(projects.domain.settings)
    api(projects.domain.tokens)
    api(projects.domain.wallets)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.appTheme.models)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.core)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.wallets.models)
    runtimeOnly(projects.domain.card)

    /** Data */
    api(projects.data.common)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** AndroidX */
    implementation(deps.androidx.activity.compose)
    implementation(deps.lifecycle.compose)

    /** Compose */
    api(deps.compose.coil)
    api(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.material3)

    /** Tangem libraries */
    implementation(tangemDeps.blockchain)
    implementation(projects.libs.blockchainSdk)

    /** Other */
    api(deps.kotlin.coroutines)
    api(deps.kotlin.immutable.collections)
    api(deps.kotlin.serialization.core)
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)
    implementation(deps.arrow.core)
    implementation(deps.jodatime)
    implementation(deps.decompose.ext.compose)
}