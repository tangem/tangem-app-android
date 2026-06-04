plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.lib.auth"
}
dependencies {
    /** Core */
    implementation(projects.core.configToggles)
    implementation(projects.core.datasource)
    implementation(projects.core.utils)

    /** Tangem libraries */
    implementation(tangemDeps.card.core)
    implementation(tangemDeps.card.android)

    /** Firebase */
    implementation(platform(deps.firebase.bom))
    implementation(deps.firebase.crashlytics)

    /** Other */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.datetime)
    implementation(deps.kotlin.serialization)
    implementation(deps.moshi)
    implementation(deps.okHttp)
    implementation(deps.retrofit)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.mockk)
}