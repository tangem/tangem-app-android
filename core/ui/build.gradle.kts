plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

dependencies {
    /** AndroidX libraries */
    implementation(deps.androidx.fragment.ktx)
    implementation(deps.androidx.paging.runtime)

    /** Compose */
    implementation(deps.compose.constraintLayout)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material)
    implementation(deps.compose.material3)
    implementation(deps.compose.paging)
    implementation(deps.compose.ui.tooling)

    /** Other libraries */
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.material)
    implementation(deps.compose.shimmer)
    implementation(deps.kotlin.immutable.collections)

    implementation(project(":core:res"))
}
