plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.transaction"
}

dependencies {
    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)

    implementation(projects.core.utils)
    implementation(projects.core.ui)

    /** Tangem SDKs */
    implementation(tangemDeps.card.core)
    implementation(tangemDeps.card.android) {
        exclude(module = "joda-time")
    }
    implementation(tangemDeps.blockchain)

    implementation(projects.domain.models)
    implementation(projects.domain.legacy)
    implementation(projects.libs.blockchainSdk)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.demo)
    implementation(projects.domain.card)
}