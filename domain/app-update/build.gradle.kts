plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    implementation(projects.core.utils)

    implementation(deps.arrow.core)

    // region Test
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.coroutine)
    testRuntimeOnly(deps.test.junit5.engine)
    // endregion
    api(deps.kotlin.coroutines)
}