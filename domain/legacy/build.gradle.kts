plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.features"
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    api(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }
    // endregion

    // region Domain
    api(projects.domain.core)
    // endregion

    // region Domain models
    api(projects.domain.models)
    api(projects.domain.tokens.models)
    api(projects.domain.transaction.models)
    // endregion

    // region Tests
    testImplementation(deps.moshi)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(projects.libs.blockchainSdk)
    // endregion
}