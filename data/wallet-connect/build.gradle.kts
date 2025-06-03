plugins {
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.android.library)
    id("configuration")
}

android {
    namespace = "com.tangem.data.walletconnect"
}

dependencies {

    /* Project - Domain */
    implementation(projects.domain.walletConnect)
    implementation(projects.domain.walletConnect.models)
    implementation(projects.domain.transaction)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.models)
    implementation(projects.domain.legacy)
    implementation(projects.libs.blockchainSdk)
    implementation(projects.data.common)

    /* Project - Data */
    implementation(projects.core.datasource)

    /* Project - Core */
    implementation(projects.core.utils)
    implementation(projects.core.analytics)

    /* DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)

    /* Tangem libraries */
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)

    /* Reown - WalletConnect */
    implementation(deps.reownCore) {
        exclude(group = "app.cash.sqldelight", module = "android-driver")
    }
    implementation(deps.reownWeb3) {
        exclude(group = "app.cash.sqldelight", module = "android-driver")
    }

    /* Other */
    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)
    implementation(projects.domain.blockaid)
    implementation(projects.domain.blockaid.models)
    implementation(projects.libs.crypto)

    /* Tests */
    testImplementation(projects.common.test)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.turbine)
}