plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.staking"
}
dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    implementation(deps.kotlin.datetime)
    implementation(deps.kotlin.serialization)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    api(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }
    // endregion

    // region Core modules
    api(projects.core.analytics)
    api(projects.core.analytics.models)
    implementation(projects.core.utils)
    // endregion

    // region Domain
    api(projects.domain.core)
    api(projects.domain.models)
    api(projects.domain.walletManager) // TODO refactor to use from data module
    // endregion

    // region Domain models
    api(projects.domain.staking.models)
    // endregion

    // region Libs
    implementation(projects.libs.blockchainSdk)
    // endregion

    // region Tests
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
    // endregion
}