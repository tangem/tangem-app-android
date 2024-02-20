plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

dependencies {

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Local storages */
    implementation(deps.androidx.datastore)

    /** Other libraries */
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.timber)

    /** Core modules */
    implementation(projects.core.datasource)

    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
}
