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
    // region sdk
    implementation(tangemDeps.blockchain)
    // endregion

    // region AndroidX libraries
    implementation(deps.androidx.datastore)
    implementation(deps.kotlin.coroutines)
    // endregion

    // region DI
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
    // endregion

    // region Arrow
    implementation(deps.arrow.core)
    // endregion

    // region Core modules
    implementation(projects.core.datasource)
    implementation(projects.core.utils)
    implementation(projects.core.ui)
    implementation(projects.libs.blockchainSdk)
    // endregion

    // region Domain modules
    implementation(projects.domain.notifications.models)
    implementation(projects.domain.notifications)
    // endregion

    // region tests
    testImplementation(deps.test.junit)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.mockk)
    testImplementation(deps.moshi)
    testImplementation(deps.moshi.kotlin)
    // endregion
}