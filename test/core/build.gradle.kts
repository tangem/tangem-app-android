plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    implementation(deps.arrow.core)

    api(deps.test.coroutine)
    api(deps.test.junit5)
    api(deps.test.mockk)
    api(deps.test.truth)
}