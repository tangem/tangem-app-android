plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {
    api(deps.arrow.core)
    api(deps.arrow.fx)
    api(deps.kotlin.coroutines)
    implementation(deps.kotlin.serialization)

    api(projects.core.analytics.models)
    api(projects.domain.models)

    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
}