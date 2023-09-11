plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    implementation(deps.kotlin.coroutines)
    implementation(projects.domain.balanceHiding.models)
}