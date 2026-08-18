plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.polymarket"
}

dependencies {

    // region Other libraries
    api(deps.arrow.core)
    api(deps.kotlin.coroutines)
    // endregion

    // region Core
    api(projects.core.pagination)
    // endregion

    // region Domain
    api(projects.domain.core)
    api(projects.domain.models)
    api(projects.domain.common)
    // endregion

    // region SDK
    implementation(projects.core.utils)
    implementation(tangemDeps.blockchain)
    // endregion

    // region Tests
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.mockk)
    testImplementation(projects.test.core)
    // endregion
}