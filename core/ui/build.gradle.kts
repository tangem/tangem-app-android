plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.core.ui"
}

dependencies {
    /** Project - Common */
    implementation(projects.common)

    /** Project - Domain */
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.appTheme.models)

    /** Project - Core */
    implementation(projects.core.res)
    implementation(projects.core.utils)
    implementation(projects.core.decompose)

    /** AndroidX libraries */
    implementation(deps.androidx.fragment.ktx)
    implementation(deps.androidx.paging.runtime)
    implementation(deps.androidx.palette)

    /** Compose */
    implementation(deps.compose.constraintLayout)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material)
    implementation(deps.compose.material3)
    implementation(deps.compose.paging)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.ui.utils)
    implementation(deps.compose.coil)
    implementation(deps.compose.navigation)
    implementation(deps.compose.navigation.hilt)

    /** Other libraries */
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.material)
    implementation(deps.compose.shimmer)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.zxing.qrCore)
    implementation(deps.jodatime)
    implementation(deps.timber)
}