plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    api(projects.core.utils)

    api(deps.arrow.core)
    api(deps.kotlin.coroutines)
    implementation(deps.hilt.core)

    testImplementation(projects.test.core)
}