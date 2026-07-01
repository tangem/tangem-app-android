plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.markets"
}


dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    implementation(tangemDeps.blockchain)
    // endregion

    // region Core modules
    api(projects.core.pagination)
    implementation(projects.core.utils)
    // endregion

    // region Domain
    api(projects.domain.common)
    api(projects.domain.quotes)
    implementation(projects.domain.card)
    // endregion

    // region Domain models
    api(projects.domain.appCurrency.models)
    api(projects.domain.markets.models)
    api(projects.domain.models)
    // endregion

    // region Libs
    api(projects.libs.blockchainSdk)
    // endregion
}