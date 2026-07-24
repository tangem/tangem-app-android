plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    api(projects.domain.account)
    api(projects.domain.models)
    implementation(deps.arrow.core)
}