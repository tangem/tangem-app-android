plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.core.decompose.ui"
}

dependencies {

    // region Compose
    implementation(deps.compose.animation)
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    // endregion

    // region Other libraries
    api(deps.decompose)
    api(deps.decompose.ext.compose)
    // endregion
}