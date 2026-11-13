plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.core.navigation"
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion
}