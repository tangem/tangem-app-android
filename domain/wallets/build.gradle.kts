plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.wallets"
}

dependencies {

    // region Domain modules
    implementation(project(":domain:legacy"))
    implementation(project(":domain:models"))
    implementation(project(":domain:wallets:models"))
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