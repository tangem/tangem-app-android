plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.core.navigation"
}

dependencies {

    // region Core modules
    api(projects.core.decompose)
    // endregion

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region DI
    implementation(deps.hilt.core)
    // endregion
}