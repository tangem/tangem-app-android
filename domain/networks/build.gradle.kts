plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Core modules
    implementation(projects.core.utils)
    // endregion

    // region Domain
    api(projects.domain.core)
    api(projects.domain.models)
    // endregion
}