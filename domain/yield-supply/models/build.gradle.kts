plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {
    // region Domain modules
    api(projects.domain.models)
    // endregion

    // region Other libraries
    implementation(deps.kotlin.serialization)
}

