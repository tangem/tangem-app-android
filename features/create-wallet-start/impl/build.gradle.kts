plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.createwalletstart.impl"
}
dependencies {
    /** Api */
    api(projects.features.createWalletStart.api)
    api(projects.features.onboardingV2.api)

    /** Project - Domain */
    api(projects.domain.card)
    api(projects.domain.common)
    api(projects.domain.hotWallet)
    api(projects.domain.settings)
    api(projects.domain.wallets)
    implementation(projects.domain.models)
    implementation(projects.domain.wallets.models)

    /** Core modules */
    api(projects.core.analytics)
    api(projects.core.datasource)
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)
    implementation(projects.core.ui)

    /** Common */
    api(projects.common.routing)
    runtimeOnly(projects.common.ui)

    /** Tangem libraries */
    implementation(tangemDeps.card.core)

    /** AndroidX libraries */
    implementation(deps.lifecycle.runtime.ktx)

    /** Compose libraries */
    api(deps.compose.animation)
    implementation(deps.compose.material3)
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.decompose.ext.compose)

    /** Other libraries */
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.lifecycle.compose)
    implementation(deps.firebase.crashlytics)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Test */
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.coroutine)
}