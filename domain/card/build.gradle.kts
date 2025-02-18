plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.card"
}

dependencies {
    implementation(projects.core.analytics.models)

    implementation(projects.domain.demo)
    implementation(projects.domain.core)
    implementation(projects.domain.legacy)
    implementation(projects.libs.blockchainSdk)
    // TODO: Remove after new card scan result was implemented
    implementation(projects.domain.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)

    implementation(tangemDeps.card.core)
    implementation(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }

}