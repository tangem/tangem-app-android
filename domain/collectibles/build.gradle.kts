plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {

    // region Other libraries
    api(deps.arrow.core)
    // endregion

    // region Core
    api(projects.core.pagination)
    api(projects.core.utils)
    // endregion

    // region Domain models
    api(projects.domain.models)
    api(projects.domain.collectibles.models)
    api(projects.domain.models)
    // endregion

    // region Tests
    testImplementation(projects.test.core)
    testImplementation(deps.kotlin.coroutines)
    // endregion
}