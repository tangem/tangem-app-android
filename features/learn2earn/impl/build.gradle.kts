plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

dependencies {
    /** Core modules */
    implementation(project(":common"))
    implementation(project(":domain:legacy"))
    implementation(project(":core:analytics"))
    implementation(project(":core:featuretoggles"))
    implementation(project(":core:datasource"))
    implementation(project(":core:utils"))
    implementation(project(":core:ui"))
    implementation(project(":core:res"))
    implementation(project(":data:source:preferences"))
    implementation(project(":libs:auth"))
    implementation(project(":libs:crypto"))

    implementation(deps.material)

    /** AndroidX */
    implementation(deps.androidx.core.ktx)
    implementation(deps.androidx.appCompat)
    implementation(deps.androidx.fragment.ktx)
    implementation(deps.androidx.activity.compose)
    implementation(deps.androidx.browser)

    /** Compose */
    implementation(deps.compose.material)
    implementation(deps.compose.material3)
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.accompanist.webView)

    /** Preferences */
    implementation(deps.krateSharedPref)

    /** Network */
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.retrofit)
    implementation(deps.retrofit.moshi)

    /** Other libraries */
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization)
    implementation(deps.timber)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}