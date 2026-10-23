plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.core.media.ui"
}

dependencies {

    // region Compose
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    // endregion

    // region Core modules
    api(projects.core.media)
    // endregion
}