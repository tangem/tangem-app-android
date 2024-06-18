plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {
    implementation(projects.domain.core)

    implementation(deps.kotlin.serialization)
    implementation(deps.jodatime)
}
