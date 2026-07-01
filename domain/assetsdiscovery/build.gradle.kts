plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.assetsdiscovery"
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    api(tangemDeps.blockchain)
    // endregion

    // region Core modules
    api(projects.core.analytics)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)
    // endregion

    // region Domain
    api(projects.domain.account.status)
    api(projects.domain.models)
    // endregion

    // region Tests
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
    // endregion
}