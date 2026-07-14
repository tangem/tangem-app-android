plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.referral.presentation"
}

dependencies {
    /** Api */
    api(projects.features.referral.api)
    api(projects.features.commonFeatures.api)
    api(projects.features.referral.domain)

    /** Core modules */
    api(projects.core.analytics)
    api(projects.core.analytics.models)
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.utils)
    api(projects.common.routing)
    implementation(projects.core.res)
    implementation(projects.core.ui)
    implementation(projects.common.ui)
    implementation(projects.common)

    /** AndroidX */
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)

    /** Compose */
    api(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui.tooling)

    /** Domain */
    api(projects.domain.account.status)
    api(projects.domain.appCurrency)
    api(projects.domain.balanceHiding)
    api(projects.domain.demo)
    api(projects.domain.wallets)
    implementation(projects.domain.account)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.card)
    implementation(projects.domain.models)
    implementation(projects.domain.wallets.models)

    /** Other libraries */
    api(deps.decompose.ext.compose)
    api(deps.kotlin.coroutines)
    implementation(deps.arrow.core)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization.core)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}