plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

dependencies {

    /** DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)

    /** Analytics - Models */
    implementation(projects.core.analytics.models)

    /** Domain */
    implementation(projects.domain.analytics)

    /** Other */
    implementation(deps.kotlin.coroutines)

    /** Core shouldn't depend on core, but in case with utils and logging its necessary */
    implementation(projects.core.utils)
}
