plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {

    // region DI
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
    // endregion

    // region Coroutines
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Time dependencies
    implementation(deps.jodatime)
    // endregion

    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.mockk)
}