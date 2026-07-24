plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.common.ui.charts"
}

dependencies {
    // region Kotlin
    api(deps.kotlin.coroutines)
    api(deps.kotlin.immutable.collections)
    // endregion

    // region Compose
    api(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.ui.utils)
    // endregion

    // region Other libraries
    implementation(deps.androidx.annotation)
    implementation(deps.jodatime)
    // endregion

    // region Vico
    implementation(tangemDeps.vico.core)
    implementation(tangemDeps.vico.compose)
    // endregion

    // region Project - Core
    implementation(projects.core.ui)
    implementation(projects.core.utils)
    // endregion
}