plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {

    // region Core modules
    implementation(projects.core.utils)
    // endregion

    // region Domain models
    api(projects.domain.models)
    // endregion
}