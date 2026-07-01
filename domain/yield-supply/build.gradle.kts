plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.yield.supply"
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
    api(projects.core.utils)
    implementation(projects.core.ui)
    // endregion

    // region Libs
    implementation(projects.libs.blockchainSdk)
    implementation(projects.libs.crypto)
    // endregion

    // region Domain
    api(projects.domain.account)
    api(projects.domain.blockaid)
    api(projects.domain.networks)
    api(projects.domain.quotes)
    api(projects.domain.transaction)
    implementation(projects.domain.account.status)
    // endregion

    // region Domain models
    api(projects.domain.appCurrency.models)
    api(projects.domain.models)
    api(projects.domain.transaction.models)
    api(projects.domain.yieldSupply.models)
    implementation(projects.domain.blockaid.models)
    // endregion

    // region Tests
    testRuntimeOnly(projects.domain.tokens)
    testImplementation(deps.kotlin.datetime)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(projects.domain.legacy)
    testImplementation(projects.test.core)
    // endregion
}