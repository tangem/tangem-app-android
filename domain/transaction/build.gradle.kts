plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.transaction"
}

dependencies {

    // region Kotlin
    api(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Tangem SDK
    api(tangemDeps.blockchain)
    api(tangemDeps.card.core)
    implementation(tangemDeps.card.android) {
        exclude(module = "joda-time")
    }
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
    api(projects.domain.account.status)
    api(projects.domain.card)
    api(projects.domain.common)
    api(projects.domain.demo)
    api(projects.domain.dynamicAddresses)
    api(projects.domain.networks)
    api(projects.domain.notifications)
    api(projects.domain.tokens)
    api(projects.domain.walletManager)
    implementation(projects.domain.core)
    implementation(projects.domain.dynamicAddresses.models)
    implementation(projects.domain.legacy)
    // endregion

    // region Domain models
    api(projects.domain.demo.models)
    api(projects.domain.models)
    api(projects.domain.transaction.models)
    api(projects.domain.wallets.models)
    // endregion

    // region Tests
    testRuntimeOnly(deps.test.junit5.vintage.engine)
    testImplementation(deps.test.junit)
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
    // endregion
}