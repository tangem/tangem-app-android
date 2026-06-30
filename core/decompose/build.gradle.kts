plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.core.decompose"
}

dependencies {

    // region DI
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
    // endregion

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region AndroidX
    api(deps.androidx.appCompat)
    // endregion

    // region Other libraries
    api(deps.decompose)
    // endregion

    // region Core modules
    api(projects.core.utils)
    // endregion
}