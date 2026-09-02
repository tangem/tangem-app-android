plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.collectibles"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core
    api(projects.core.utils)
    implementation(projects.core.pagination)
    // endregion

    // region Grow
    implementation(projects.grow.datasource)
    // endregion

    // region Domain
    implementation(projects.domain.models)
    api(projects.domain.collectibles)
    implementation(projects.domain.collectibles.models)
    // endregion

    // region Tests
    testImplementation(projects.test.core)
    testImplementation(projects.domain.models)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    // endregion
}