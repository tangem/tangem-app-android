plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.account.status"
}
dependencies {
    api(projects.domain.account)
    api(projects.domain.card)
    api(projects.domain.core)
    api(projects.domain.common)
    api(projects.domain.express)
    api(projects.domain.quotes)
    api(projects.domain.models)
    api(projects.domain.networks)
    api(projects.domain.nft)
    api(projects.domain.referral)
    api(projects.domain.staking)
    api(projects.domain.tokens)
    api(projects.domain.tokens.models)
    api(projects.domain.visa)
    api(projects.domain.walletManager)
    api(projects.domain.wallets)
    api(projects.core.analytics)
    api(projects.domain.yieldSupply.models)
    api(deps.arrow.core)
    api(deps.kotlin.coroutines)

    implementation(projects.libs.blockchainSdk)
    implementation(projects.libs.crypto)
    implementation(projects.core.utils)
    implementation(projects.core.analytics.models)
    implementation(projects.domain.express.models)

    implementation(deps.kotlin.serialization)

    implementation(tangemDeps.blockchain)

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // end

    testImplementation(projects.common) // reads the same total helpers the UI does, for seam parity tests
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
    testImplementation(projects.test.mock)
    testImplementation(projects.domain.wallets.models)
    testImplementation(projects.domain.staking.models)
    testImplementation(deps.arrow.core)
    testImplementation(deps.kotlin.coroutines)
    testImplementation(deps.kotlin.datetime)
}