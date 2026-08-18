plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {
    api(projects.core.utils)
    api(deps.kotlin.serialization.core)
    api(deps.moshi)
}