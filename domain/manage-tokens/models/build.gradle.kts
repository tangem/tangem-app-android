plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {

    /* Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.tokens.models)

    /** Other */
    implementation(deps.kotlin.serialization)
}