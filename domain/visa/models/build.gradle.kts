plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {
    implementation(deps.moshi.kotlin)
    implementation(deps.moshi.adapters)
    implementation(deps.kotlin.serialization)
}