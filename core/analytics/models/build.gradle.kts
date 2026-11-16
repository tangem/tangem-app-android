plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {
    api(deps.kotlin.serialization)

    testImplementation(deps.test.junit5)
    testImplementation(deps.test.truth)
}