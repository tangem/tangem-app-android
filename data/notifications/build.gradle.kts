plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.notifications"
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    implementation(deps.androidx.datastore)
    implementation(deps.arrow.core)
    implementation(deps.moshi)
    // endregion

    // region SDK
    implementation(tangemDeps.blockchain)
    // endregion

    // region DI
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core modules
    api(projects.core.datasource)
    api(projects.core.utils)
    implementation(projects.common.ui)
    implementation(projects.libs.blockchainSdk)
    // endregion

    // region Domain modules
    api(projects.domain.notifications)
    implementation(projects.domain.models)
    implementation(projects.domain.notifications.models)
    // endregion

    // region tests
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    // endregion
}