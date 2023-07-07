plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

dependencies {
    /** AndroidX */
    implementation(deps.androidx.activity.compose)

    /** Compose */
    implementation(deps.compose.coil)
    implementation(deps.compose.constraintLayout)
    implementation(deps.compose.material)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.navigation)
    implementation(deps.compose.navigation.hilt)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.shimmer)
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.compose.reorderable)

    /** Other libraries */
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.tangem.blockchain)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Core modules */
    implementation(project(":core:featuretoggles"))
    implementation(project(":core:ui"))

    /** Feature Apis */
    implementation(project(":features:wallet:api"))

    /** Domain modules */
    implementation(project(":domain:legacy"))
}
