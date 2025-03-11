plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.staking"
}

dependencies {
    api(projects.domain.staking.models)
    api(projects.domain.core)
    api(projects.core.analytics)
    api(projects.core.utils)

    implementation(deps.kotlin.serialization)
    implementation(deps.jodatime)

    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)

    implementation(projects.features.staking.api)

    implementation(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }
}