plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.pushnotificationpreferences"
}

dependencies {
    /** Domain */
    implementation(projects.domain.pushNotificationPreferences)
    implementation(projects.domain.models)

    /** Core */
    implementation(projects.core.datasource)
    implementation(projects.core.utils)

    /** Other */
    implementation(deps.androidx.datastore)
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(deps.test.junit)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.turbine)
    testImplementation(deps.moshi)
    testImplementation(deps.moshi.kotlin)
}