plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.promo"
}

dependencies {
    implementation(projects.core.remote)

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.datetime)
    // endregion

    // region Other libraries
    implementation(deps.arrow.core)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    runtimeOnly(deps.room.runtime)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core
    implementation(projects.store.datasource)
    implementation(projects.core.local)
    api(projects.core.utils)
    // endregion

    // region Data
    implementation(projects.data.common)
    // endregion

    // region Domain
    api(projects.domain.promo)
    api(deps.moshi)
    // endregion

    // region Domain models
    implementation(projects.domain.models)
    implementation(projects.domain.promo.models)
    // endregion

    // region Tests
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(deps.moshi.kotlin)
    testImplementation(projects.test.core)
    testImplementation(projects.common.test)
    // endregion
}