plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

dependencies {
    api(projects.core.utils)

    api(deps.decompose)
    implementation(deps.kotlin.coroutines)

    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
}