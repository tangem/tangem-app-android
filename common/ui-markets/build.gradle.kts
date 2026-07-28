plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.common.ui.markets"
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    api(deps.kotlin.immutable.collections)
    // endregion

    // region Compose
    api(deps.compose.foundation)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui.utils)
    // endregion

    // region Other libraries
    implementation(deps.androidx.annotation)
    implementation(deps.arrow.core)
    implementation(deps.haze)
    implementation(deps.hilt.android)
    // endregion

    // region Project - Core
    api(projects.core.analytics)
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.ui)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)
    // endregion

    // region Project - Common
    api(projects.common.routing)
    api(projects.common.uiCharts)
    implementation(projects.common.ui)
    // endregion

    // region Project - Domain
    api(projects.domain.appCurrency.models)
    api(projects.domain.card)
    api(projects.domain.demo)
    api(projects.domain.feedback)
    api(projects.domain.models)
    api(projects.domain.offramp)
    api(projects.domain.tokens)
    implementation(projects.domain.onramp.models)
    implementation(projects.domain.staking)
    implementation(projects.domain.tokens.models)
    // endregion

    // region Test
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(projects.common.test)
    // endregion
}