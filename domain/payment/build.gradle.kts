plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    /** Project - Domain */
    api(projects.domain.models)
    implementation(projects.domain.payment.models)
}