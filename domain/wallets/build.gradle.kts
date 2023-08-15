plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.wallets"
}

dependencies {

    // region Core modules
    implementation(projects.core.res)
    // endregion

    // region Domain modules
    implementation(projects.domain.legacy)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)
    // endregion

    // region Tangem libraries
    implementation(deps.tangem.blockchain) // android-library
    implementation(deps.tangem.card.core)
    // endregion

    // region Other libraries
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    // endregion
}