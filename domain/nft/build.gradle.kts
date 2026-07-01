plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.nft"
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    // endregion

    // region Core modules
    api(projects.core.analytics.models)
    api(projects.core.utils)
    // endregion

    // region Domain
    api(projects.domain.account)
    api(projects.domain.networks)
    api(projects.domain.quotes)
    api(projects.domain.tokens)
    api(projects.domain.wallets)
    implementation(projects.domain.core)
    // endregion

    // region Domain models
    api(projects.domain.models)
    api(projects.domain.nft.models)
    // endregion
}