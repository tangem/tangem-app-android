plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {
    api(deps.kotlin.coroutines)
    api(projects.core.utils)
    api(deps.kotlin.coroutines)
    api(deps.kotlin.serialization.core)
    api(deps.moshi)
    api(deps.moshi.adapters)
    implementation(deps.hilt.core)
}