plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.common.test"
}

dependencies {

    // region Other libraries
    implementation(deps.jodatime)
    // endregion

    // region Tangem SDK
    api(tangemDeps.blockchain)
    api(tangemDeps.card.core)
    // endregion

    // region Core
    api(projects.core.datasource)
    implementation(projects.core.utils)
    // endregion

    // region Data
    implementation(projects.data.common)
    // endregion

    // region Domain
    api(projects.domain.card)
    api(projects.domain.models)
    implementation(projects.domain.wallets)
    // endregion

    // region Domain models
    api(projects.domain.staking.models)
    // endregion

    // region Libs
    api(projects.libs.blockchainSdk)
    // endregion
}