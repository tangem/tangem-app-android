plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

dependencies {

    /** DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)

    /** Core shouldn't depends on core, but in case with utils and logging its necessary */
    implementation(projects.core.utils)
    implementation(projects.core.analytics.models)

    implementation(deps.kotlin.coroutines)
}
