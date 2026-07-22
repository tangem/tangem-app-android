plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.libs.auth"
}

dependencies {

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Kotlin
    api(deps.kotlin.datetime)
    api(deps.kotlin.serialization)
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    api(deps.okHttp)
    implementation(deps.moshi)
    implementation(deps.retrofit)
    // endregion

    // region Firebase
    implementation(platform(deps.firebase.bom))
    implementation(deps.firebase.crashlytics)
    // endregion

    // region Tangem
    implementation(tangemDeps.card.android)
    implementation(tangemDeps.card.core)
    // endregion

    // region Core modules
    implementation(projects.core.configToggles)
    implementation(projects.core.datasource)
    implementation(projects.core.utils)
    // endregion

    // region Tests
    testImplementation(deps.androidx.datastore)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    // endregion
}