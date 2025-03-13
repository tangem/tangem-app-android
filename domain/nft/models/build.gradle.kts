plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)

    implementation(projects.domain.models)

    implementation(projects.domain.tokens.models)
}