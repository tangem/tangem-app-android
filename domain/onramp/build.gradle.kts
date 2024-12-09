plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {
    /** Core modules */
    implementation(projects.core.analytics.models)

    api(projects.domain.onramp.models)
    api(projects.domain.tokens.models)
    api(projects.domain.wallets.models)

    api(projects.domain.core)
    api(projects.domain.settings)
    implementation(deps.kotlin.serialization)
}