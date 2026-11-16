plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.core.navigation.impl"
}

dependencies {

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region AndroidX
    implementation(deps.androidx.core)
    implementation(deps.androidx.core.ktx)
    // endregion

    // region Core modules
    api(projects.core.navigation)
    implementation(projects.core.decompose)
    implementation(projects.core.utils)
    // endregion

    // region Tests
    testImplementation(projects.test.core)
    // endregion
}