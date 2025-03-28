plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.core.ui"
}

dependencies {
    /** Project - Domain */
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.appTheme.models)

    /** Project - Core */
    implementation(projects.core.res)
    implementation(projects.core.utils)
    implementation(projects.core.decompose)
    implementation(projects.core.error)

    /** AndroidX libraries */
    implementation(deps.androidx.fragment.ktx)
    implementation(deps.androidx.paging.runtime)
    implementation(deps.lifecycle.runtime.ktx)
    implementation(deps.androidx.palette)
    implementation(deps.androidx.windowManager) {
        exclude(
            deps.kotlin.coroutines.android.get().module.group,
            deps.kotlin.coroutines.android.get().module.name
        )
    }

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
    implementation(deps.compose.reorderable)

    /** Other libraries */
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.compose.accompanist.permission)
    implementation(deps.material)
    implementation(deps.compose.shimmer)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.zxing.qrCore)
    api(deps.jodatime)
    implementation(deps.timber)
    implementation(deps.markdown)

    /** Tests */
    testImplementation(deps.test.junit)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
}