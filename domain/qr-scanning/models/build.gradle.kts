plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}
dependencies {

    // region Domain models
    api(projects.domain.models)
    // endregion
}