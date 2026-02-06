plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {
    api(projects.domain.core)
    api(projects.domain.models)
    api(projects.core.pagination)
    implementation(projects.domain.common)
    implementation(projects.domain.networks)
    implementation(deps.kotlin.serialization)
}