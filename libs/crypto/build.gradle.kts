plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.lib.crypto"
}

dependencies {

    /** Coroutines */
    implementation(deps.kotlin.coroutines)

    /** SDK */
    implementation(tangemDeps.blockchain)

    /** Core */
    implementation(projects.core.utils)

    /** Libs */
    implementation(projects.libs.blockchainSdk)
}