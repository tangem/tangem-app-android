plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.storiesv2.impl"
}

dependencies {
    /** Core */
    api(projects.core.decompose)
    api(projects.core.utils)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.ui)

    /** Api */
    api(projects.features.storiesV2.api)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.decompose.ext.compose)

    /** Media */
    implementation(deps.androidx.media3.common)
    implementation(deps.androidx.media3.datasource)
    implementation(deps.androidx.media3.exoplayer)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Other */
    implementation(deps.androidx.annotation)
    // `@UnstableApi` in media3 is an androidx opt-in marker, so opting in needs androidx's own OptIn annotation.
    implementation(deps.androidx.annotation.experimental)
    implementation(deps.androidx.appCompat)
    implementation(deps.compose.coil)
    implementation(deps.decompose)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization.core)
    implementation(deps.lifecycle.compose)
    implementation(deps.lottie)
    implementation(deps.lottie.compose)

    /** Tests */
    testImplementation(projects.test.core)
}