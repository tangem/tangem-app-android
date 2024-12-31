plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}
dependencies {
    implementation(projects.core.utils)
}