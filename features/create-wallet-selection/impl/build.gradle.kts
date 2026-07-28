plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.createwalletselection.impl"
}

dependencies {
    /** Api */
    implementation(projects.features.createWalletSelection.api)

    /** Core modules */
    api(projects.core.analytics)
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)
    implementation(projects.core.ui)

    /** Project - Domain */
    api(projects.domain.hotWallet)
    api(projects.domain.wallets)

    /** Common */
    implementation(projects.common)
    implementation(projects.common.routing)

    /** Compose libraries */
    api(deps.compose.animation)
    implementation(deps.compose.material3)
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.decompose.ext.compose)

    /** Other libraries */
    implementation(deps.androidx.appCompat)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.lifecycle.compose)
    implementation(deps.lifecycle.runtime.ktx)
    implementation(deps.firebase.crashlytics)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}