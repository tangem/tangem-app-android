plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    implementation(projects.domain.core)
    implementation(deps.kotlin.coroutines)
    implementation(projects.domain.settings)
    implementation(projects.domain.balanceHiding.models)
}