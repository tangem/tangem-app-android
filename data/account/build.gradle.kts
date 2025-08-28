plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.account"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {

    // region Project - Core
    implementation(projects.core.datasource)
    api(projects.core.utils)
    // endregion

    // region Project - Domain
    api(projects.domain.account)
    api(projects.domain.models)
    // endregion

    // region Project - Data
    implementation(projects.data.common)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region AndroidX libraries
    implementation(deps.androidx.datastore)
    // endregion

    // region Other Dependencies
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.timber)
    // endregion

    // region Test
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(projects.common.test)
    // endregion
}