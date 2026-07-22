plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.txhistory"
}

dependencies {

    implementation(projects.domain.legacy)
    implementation(projects.domain.common)
    implementation(projects.domain.walletManager)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.txhistory)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.express)
    implementation(projects.domain.express.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.onramp)
    implementation(projects.domain.onramp.models)
    implementation(projects.domain.account)
    implementation(projects.domain.account.status)
    implementation(projects.domain.visa)
    implementation(projects.domain.visa.models)

    // region Other libraries
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.paging.runtime)
    implementation(deps.arrow.core)
    implementation(deps.jodatime)
    implementation(deps.room.runtime)
    // endregion

    // region Tangem SDK
    implementation(tangemDeps.blockchain)
    // endregion

    // region DI
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core modules
    api(projects.core.analytics)
    api(projects.core.datasource)
    api(projects.core.utils)
    implementation(projects.core.pagination)
    // endregion

    // region Data
    api(projects.data.common)
    // endregion

    // region Domain
    api(projects.domain.account)
    api(projects.domain.common)
    api(projects.domain.express)
    api(projects.domain.models)
    api(projects.domain.onramp)
    api(projects.domain.txhistory)
    api(projects.domain.walletManager)
    api(projects.domain.wallets)
    implementation(projects.domain.account.status)
    implementation(projects.domain.legacy)
    implementation(projects.domain.visa)
    // endregion

    // region Domain models
    implementation(projects.domain.express.models)
    implementation(projects.domain.onramp.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.visa.models)
    implementation(projects.domain.wallets.models)
    // endregion

    // region Libs
    api(projects.libs.blockchainSdk)
    // endregion

    // region Test
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
    testImplementation(projects.test.mock)
    // endregion
}