plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {

    // region Domain
    api(projects.domain.core)
    api(projects.domain.models)
    api(deps.kotlin.coroutines)
    // endregion

    // region Tests
    testImplementation(projects.test.core)
    // endregion
}