plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.common.test"
}

dependencies {
    implementation(projects.core.datasource)
    implementation(projects.core.utils)

    implementation(projects.data.common)
    implementation(projects.data.staking)

    implementation(projects.domain.legacy)
    implementation(projects.domain.models)
    implementation(projects.domain.card)
    implementation(projects.domain.staking.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)

    implementation(projects.libs.blockchainSdk)

    implementation(deps.androidx.datastore)
    implementation(deps.jodatime)
    implementation(deps.test.coroutine)

    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)

    implementation(deps.test.junit5)
    implementation(deps.test.truth)
}