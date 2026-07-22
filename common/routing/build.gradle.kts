plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.common.routing"
}

dependencies {

    // region Kotlin
    api(deps.kotlin.serialization)
    // endregion

    // region Core
    api(projects.core.analytics.models)
    api(projects.core.decompose)
    api(projects.core.utils)
    // endregion

    // region Domain
    api(projects.domain.appCurrency.models)
    api(projects.domain.feedback.models)
    api(projects.domain.markets.models)
    api(projects.domain.models)
    api(projects.domain.nft.models)
    api(projects.domain.onramp.models)
    api(projects.domain.staking)
    api(projects.domain.tokens.models)
    implementation(projects.domain.visa.models)
    // endregion

    // region Tests
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    // endregion
}