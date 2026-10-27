plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.core.analytics.impl"
}

dependencies {

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Analytics SDKs
    implementation(deps.amplitude)
    implementation(deps.customerio.analytics)
    implementation(deps.customerio.messaging)
    // endregion

    // region Tangem
    implementation(tangemDeps.card.core) // for calculating user id hash
    // endregion

    // region Core modules
    api(projects.core.analytics)
    // Core shouldn't depend on core, but with utils and logging it's necessary.
    implementation(projects.core.utils)
    // endregion

    // region Domain
    implementation(projects.domain.analytics)
    // endregion

    // region Tests
    testImplementation(projects.test.core)
    // endregion
}