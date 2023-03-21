plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

dependencies {
    implementation(project(":core:datasource"))

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.timber)

    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
}
