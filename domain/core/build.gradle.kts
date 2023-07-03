plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    api(deps.kotlin.coroutines)
    api(deps.arrow.core)
    api(deps.arrow.fx)

    testImplementation(deps.test.junit)
    testImplementation(deps.test.coroutine)
}
