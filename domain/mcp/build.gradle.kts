plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    api(projects.domain.models)
    api(projects.domain.mcp.models)

    api(deps.arrow.core)
    api(deps.kotlin.coroutines)

    testImplementation(projects.test.core)
}