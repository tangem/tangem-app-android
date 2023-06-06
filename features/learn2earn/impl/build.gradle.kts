plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

dependencies {
    /** AndroidX */
    implementation(deps.androidx.activity.compose)
    implementation(deps.androidx.core.ktx)
    implementation(deps.androidx.appCompat)
    implementation(deps.androidx.constraintLayout)

    /** Compose */
    implementation(deps.compose.material)
    implementation(deps.compose.foundation)
    implementation(deps.compose.constraintLayout)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.animation)
    implementation(deps.compose.navigation)
    implementation(deps.compose.navigation.hilt)

    /** Core modules */
    implementation(project(":common"))
    implementation(project(":core:featuretoggles"))
    implementation(project(":core:datasource"))
    implementation(project(":core:analytics"))
    implementation(project(":core:utils"))
    implementation(project(":core:ui"))
    implementation(project(":core:res"))

    /** Tangem libraries */
    // TODO: fixme: delete if later it doesn't needed
    // implementation(deps.tangem.card.core)
    // implementation(deps.tangem.card.android) {
    //     exclude(module = "joda-time")
    // }

    /** Other libraries */
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization)
    implementation(deps.timber)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}