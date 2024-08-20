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
    implementation(deps.kotlin.serialization)

    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)

    implementation(projects.features.staking.api)

    implementation(deps.tangem.blockchain) {
        exclude(module = "joda-time")
    }
}