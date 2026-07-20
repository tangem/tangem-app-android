plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.polymarket"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)
    // endregion

    // region Tangem SDK
    implementation(tangemDeps.card.core)
    // AndroidSecureStorageV2 for Polymarket API credentials (see com.tangem.data.polymarket.store).
    implementation(tangemDeps.card.android) {
        exclude(module = "joda-time")
    }
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core
    api(projects.core.datasource)
    api(projects.core.utils)
    // endregion

    // region Domain
    api(projects.domain.polymarket)
    // endregion

    // region tests
    testImplementation(deps.moshi.kotlin)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    // endregion
}