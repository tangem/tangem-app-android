plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.onboarding.legacy"
}

dependencies {
    /** Core modules */
    implementation(projects.common)
    implementation(projects.domain.models)
    implementation(projects.core.featuretoggles)
    implementation(projects.core.datasource)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.utils)
    implementation(projects.core.ui)
    implementation(projects.core.res)
    implementation(projects.libs.tangemSdkApi)
    implementation(projects.libs.blockchainSdk)
    implementation(projects.common.routing)

    /** Tangem libraries */
    implementation(deps.tangem.card.core)
    implementation(deps.tangem.blockchain)
    implementation(deps.tangem.card.android) {
        exclude(module = "joda-time")
    }

    /** Domain */
    implementation(projects.domain.card)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.legacy)
    implementation(projects.domain.demo)
    implementation(projects.domain.settings)

    /** AndroidX libraries */
    implementation(deps.androidx.core.ktx)
    implementation(deps.androidx.appCompat)
    implementation(deps.androidx.fragment.ktx)
    implementation(deps.androidx.constraintLayout)
    implementation(deps.androidx.activity.compose)
    implementation(deps.lifecycle.runtime.ktx)
    implementation(deps.lifecycle.common.java8)
    implementation(deps.lifecycle.viewModel.ktx)

    /** Compose libraries */
    implementation(deps.compose.material)
    implementation(deps.compose.material3)
    implementation(deps.compose.animation)
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.coil)
    implementation(deps.compose.shimmer)

    /** Other libraries */
    implementation(deps.compose.shimmer)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization)
    implementation(deps.timber)
    implementation(deps.reKotlin)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}