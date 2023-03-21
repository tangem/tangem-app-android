plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

dependencies {
    /** AndroidX libraries */
    implementation(deps.androidx.fragment.ktx)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material)
    implementation(deps.compose.ui.tooling)

    /** Other libraries */
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.material)
    implementation(deps.compose.shimmer)

    implementation(project(":core:res"))
}
