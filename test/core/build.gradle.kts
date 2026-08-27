plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    api(projects.core.local)
    api(projects.core.utils)
    api(projects.domain.core)
    api(deps.arrow.core)
    api(deps.kotlin.coroutines)

    api(deps.androidx.datastore.core)
    api(deps.test.coroutine)
    api(deps.test.junit5)
    api(deps.test.mockk)
    api(deps.test.truth)
    api(deps.test.turbine)
}