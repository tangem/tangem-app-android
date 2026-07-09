plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {
    api(deps.arrow.core)
    api(deps.kotlin.coroutines)

    api(projects.core.analytics.models)
    api(projects.domain.models)

    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.mockk)
}