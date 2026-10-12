plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {
    api(deps.kotlin.coroutines)
    api(deps.kotlin.serialization)
    api(deps.okHttp)
    implementation(projects.core.utils)
    implementation(deps.okio)

    testImplementation(projects.core.remote)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.coroutine)
}