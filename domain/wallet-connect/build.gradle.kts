plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.walletconnect"
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    api(tangemDeps.blockchain)
    api(tangemDeps.card.core)
    // endregion

    // region Core modules
    api(projects.core.analytics)
    api(projects.core.analytics.models)
    implementation(projects.core.utils)
    // endregion

    // region Domain
    implementation(projects.domain.core)
    implementation(projects.domain.transaction)
    // endregion

    // region Domain models
    api(projects.domain.blockaid.models)
    api(projects.domain.models)
    api(projects.domain.transaction.models)
    api(projects.domain.walletConnect.models)
    // endregion
}