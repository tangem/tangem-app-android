plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {

    /** Domain */
    implementation(projects.domain.qrScanning.models)

    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)
}