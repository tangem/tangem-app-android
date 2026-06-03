plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    implementation(projects.core.utils)
    implementation(deps.arrow.core)

    api(deps.androidx.datastore.core)
    api(deps.test.coroutine)
    api(deps.test.junit5)
    api(deps.test.mockk)
    api(deps.test.truth)
    api(deps.test.turbine)
}