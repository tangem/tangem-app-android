plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {

    /** Coroutines */
    implementation(deps.kotlin.coroutines)

    /** SDK */
    implementation(deps.tangem.blockchain)

    /** Core */
    implementation(projects.core.utils)
}