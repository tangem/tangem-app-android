plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {
    api(projects.domain.onramp.models)

    api(projects.domain.core)
    implementation(deps.kotlin.serialization)
}
