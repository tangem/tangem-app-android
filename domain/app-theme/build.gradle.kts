plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {

    /** Project - Domain */
    implementation(projects.core.utils)
    implementation(projects.domain.core)
    implementation(projects.domain.appTheme.models)
}