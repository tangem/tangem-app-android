plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.common.routing"
}

kotlin {
    compilerOptions {
        // Interface defaults (e.g. DeepLinkHandler.isEnabled) become JVM default methods, so implementors in other
        // modules need no compiled bridges and stay valid when a default member is added
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

dependencies {

    // region Kotlin
    api(deps.kotlin.serialization)
    // endregion

    // region DI
    implementation(deps.hilt.core)
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
    implementation(projects.domain.notifications.models)
    api(projects.domain.onramp.models)
    api(projects.domain.staking)
    api(projects.domain.tokens.models)
    implementation(projects.domain.visa.models)
    // endregion

    // region Tests
    testImplementation(projects.test.core)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    // endregion
}