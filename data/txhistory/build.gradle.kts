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
    implementation(projects.data.common)

    implementation(projects.core.utils)
    implementation(projects.core.datasource)
    implementation(projects.core.pagination)

    implementation(projects.domain.legacy)
    implementation(projects.domain.common)
    implementation(projects.domain.walletManager)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.txhistory)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.wallets.models)

    implementation(projects.libs.blockchainSdk)

    implementation(deps.kotlin.coroutines)
    implementation(deps.androidx.paging.runtime)
    implementation(deps.jodatime)
    implementation(tangemDeps.blockchain)

    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
}