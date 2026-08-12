plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    // endregion

    // region Domain
    api(projects.domain.account)
    api(projects.domain.common)
    api(projects.domain.networks)
    // endregion

    // region Domain models
    api(projects.domain.models)
    api(projects.domain.qrScanning.models)
    // endregion
    implementation(projects.domain.core)
}