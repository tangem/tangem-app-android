plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.tokens"
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

    // region Core modules
    api(projects.core.utils)
    // endregion

    // region Domain
    api(projects.domain.common)
    api(projects.domain.core)
    api(projects.domain.express)
    api(projects.domain.legacy)
    api(projects.domain.networks)
    api(projects.domain.quotes)
    api(projects.domain.staking)
    api(projects.domain.stories)
    api(projects.domain.visa)
    api(projects.domain.walletManager)
    implementation(projects.domain.card)
    // endregion

    // region Domain models
    api(projects.domain.models)
    api(projects.domain.tokens.models)
    api(projects.domain.transaction.models)
    api(projects.domain.yieldSupply.models)
    implementation(projects.domain.express.models)
    implementation(projects.domain.stories.models)
    // endregion

    // region Features
    api(projects.features.virtualAccounts.details.api) // VIRTUAL_ACCOUNTS_ENABLED
    // endregion

    // region Libs
    implementation(projects.libs.crypto)
    // endregion

    // region Common
    implementation(projects.common)
    // endregion

    // region Runtime
    runtimeOnly(projects.domain.settings)
    // endregion

    // region Tests
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
    // endregion
}