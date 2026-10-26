plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.core.media"
}

dependencies {

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Media
    implementation(deps.androidx.media3.common)
    implementation(deps.androidx.media3.datasource)
    implementation(deps.androidx.media3.exoplayer)
    // endregion

    // region Other libraries
    implementation(deps.androidx.annotation)
    // Opting in to media3's `@UnstableApi` takes androidx's own `OptIn`.
    implementation(deps.androidx.annotation.experimental)
    // endregion

    // region Core modules
    implementation(projects.core.utils)
    // endregion

    // region Tests
    testImplementation(projects.test.core)
    // endregion
}