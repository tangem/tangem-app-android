plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.libs.crypto"
}

dependencies {

    // region Tangem SDKs
    api(tangemDeps.blockchain)
    api(tangemDeps.card.core)
    // endregion

    // region Project
    implementation(projects.core.utils)
    api(projects.domain.models)
    api(projects.libs.blockchainSdk)
    // endregion

    // region Test libraries
    testImplementation(projects.test.core)
    // endregion
}