plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    api(projects.domain.core)
    api(projects.domain.models)
    api(projects.domain.tokens.models)
}