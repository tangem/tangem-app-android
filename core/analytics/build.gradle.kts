plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {

    // region Core modules
    api(projects.core.analytics.models)
    // endregion

    // region Domain models
    api(projects.domain.models)
    // endregion

    // region Tests
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.truth)
    // endregion
}