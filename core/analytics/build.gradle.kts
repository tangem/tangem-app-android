plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

dependencies {

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Core shouldn't depends on core, but in case with utils and logging its necessary */
    implementation(projects.core.utils)

    implementation(projects.core.analytics.models)
}