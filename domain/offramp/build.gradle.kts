plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}
dependencies {

    // region Other libraries
    api(deps.arrow.core)
    // endregion

    // region Core modules
    implementation(projects.core.utils)
    // endregion

    // region Domain models
    api(projects.domain.models)
    // endregion

    // region Tests
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    // endregion
}
