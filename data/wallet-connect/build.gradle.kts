plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.data.walletconnect"
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    api(deps.moshi)
    ksp(deps.moshi.kotlin.codegen)
    implementation(deps.jodatime)
    implementation(deps.okio)
    // endregion

    // region Reown - WalletConnect
    api(deps.reownWeb3) {
        exclude(group = "app.cash.sqldelight", module = "android-driver")
    }
    implementation(deps.reownCore) {
        exclude(group = "app.cash.sqldelight", module = "android-driver")
    }
    // endregion

    // region Tangem libraries
    api(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)
    // endregion

    // region DI
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
    // endregion

    // region Project - Core
    api(projects.core.analytics)
    api(projects.core.configToggles)
    api(projects.core.datasource)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)
    // endregion

    // region Project - Data
    implementation(projects.data.common)
    // endregion

    // region Project - Domain
    api(projects.domain.account)
    api(projects.domain.account.status)
    api(projects.domain.blockaid)
    api(projects.domain.common)
    api(projects.domain.transaction)
    api(projects.domain.walletConnect)
    api(projects.domain.walletConnect.models)
    api(projects.domain.walletManager)
    api(projects.domain.wallets)
    implementation(projects.domain.core)
    implementation(projects.domain.models)
    runtimeOnly(projects.domain.tokens)
    // endregion

    // region Project - Domain models
    implementation(projects.domain.blockaid.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.transaction.models)
    // endregion

    // region Project - Libs
    api(projects.libs.blockchainSdk)
    implementation(projects.libs.crypto)
    // endregion

    // region Tests
    testImplementation(projects.common.test)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.turbine)
    // endregion
}