plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.details.impl"
}
dependencies {

    /* Project - API */
    api(projects.features.addressBook.api)
    api(projects.features.details.api)
    api(projects.features.onboardingV2.api)
    api(projects.features.wallet.api)
    implementation(projects.features.virtualAccounts.details.api)

    /* Project - Core */
    api(projects.core.analytics)
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)
    implementation(projects.core.ui)

    /* Common */
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /* Project - Domain */
    api(projects.domain.card)
    api(projects.domain.feedback)
    api(projects.domain.settings)
    api(projects.domain.virtualAccount)
    api(projects.domain.visa)
    api(projects.domain.walletConnect)
    api(projects.domain.wallets)
    implementation(projects.domain.common)
    implementation(projects.domain.feedback.models)
    implementation(projects.domain.models)
    implementation(projects.domain.virtualAccount.models)
    runtimeOnly(projects.domain.appCurrency)
    runtimeOnly(projects.domain.balanceHiding)
    runtimeOnly(projects.domain.tokens)
    implementation(projects.domain.virtualAccount)

    /* SDK */
    // TODO: For TangemError model, should be removed after card domain scanning refactoring
    implementation(tangemDeps.card.core)

    /* AndroidX */
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)
    implementation(deps.lifecycle.compose)

    /* Compose */
    api(deps.compose.foundation)
    implementation(deps.compose.animation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.material3)
    implementation(deps.compose.coil)
    implementation(deps.compose.reorderableV2)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /* Other */
    implementation(deps.arrow.core)
    implementation(deps.arrow.fx)
    implementation(deps.decompose)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)

    /* Test */
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.coroutine)
}