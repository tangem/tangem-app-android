plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    implementation(projects.domain.core)
    // TODO: Remove after new card scan result was implemented
    implementation(projects.domain.models)

    implementation(projects.core.analytics.models)

    implementation(deps.tangem.card.core)
}