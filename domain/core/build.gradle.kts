plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {
    api(deps.kotlin.coroutines)
    api(deps.arrow.core)
    api(deps.arrow.fx)

    implementation(deps.kotlin.serialization)
}