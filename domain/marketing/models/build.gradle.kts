plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    testImplementation(projects.test.core)
}