plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.send.impl"
}

dependencies {
    /** AndroidX */
    implementation(deps.androidx.fragment.ktx)
    implementation(deps.androidx.appCompat)
    implementation(deps.androidx.paging.runtime)

    /** Other dependencies */
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.material)
    implementation(deps.arrow.core)
    implementation(deps.lifecycle.compose)
    implementation(deps.jodatime)

    /** Compose */
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.compose.material3)
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.navigation)
    implementation(deps.compose.navigation.hilt)
    implementation(deps.compose.paging)
    implementation(deps.compose.constraintLayout)

    /** Tangem SDKs */
    implementation(deps.tangem.card.core)
    implementation(deps.tangem.blockchain)

    /** Core modules */
    implementation(projects.core.featuretoggles)
    implementation(projects.core.ui)
    implementation(projects.core.utils)
    implementation(projects.core.navigation)

    /** Domain modules */
    implementation(projects.domain.models)
    implementation(projects.domain.legacy)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.txhistory)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.transaction)
    implementation(projects.domain.card)
    implementation(projects.domain.demo)

    /** Feature modules */
    implementation(projects.features.send.api)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}