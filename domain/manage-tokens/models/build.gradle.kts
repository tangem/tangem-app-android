plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {

    /* Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.tokens.models)
}