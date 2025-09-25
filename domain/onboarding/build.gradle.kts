plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)

    implementation(projects.core.analytics)
    implementation(projects.domain.models)
}