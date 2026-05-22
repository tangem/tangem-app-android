plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.lib.auth"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    /** Core */
    implementation(projects.core.utils)
    implementation(projects.core.configToggles)

    /** Tangem libraries */
    implementation(tangemDeps.card.core)

    /** Firebase */
    implementation(platform(deps.firebase.bom))
    implementation(deps.firebase.crashlytics)

    /** Other */
    implementation(deps.arrow.core)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.mockk)
}