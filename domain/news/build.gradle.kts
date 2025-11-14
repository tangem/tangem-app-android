plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {
    api(projects.domain.core)
    api(projects.domain.models)
    implementation(deps.kotlin.serialization)
}