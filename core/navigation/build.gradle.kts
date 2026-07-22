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
    implementation(projects.core.utils)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    // endregion

    // region AndroidX
    implementation(deps.androidx.core)
    // endregion
}