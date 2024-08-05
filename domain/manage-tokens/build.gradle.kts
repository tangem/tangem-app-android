plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    api(projects.domain.manageTokens.models)
    api(projects.domain.core)
}