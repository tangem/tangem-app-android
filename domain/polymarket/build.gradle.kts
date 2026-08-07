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
    api(projects.domain.account.status)
    // endregion

    // region SDK
    implementation(projects.libs.blockchainSdk)
    implementation(projects.core.utils)
    implementation(projects.domain.account)
    implementation(tangemDeps.blockchain)
    // endregion

    // region Tests
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.mockk)
    testImplementation(projects.test.core)
    // endregion
}