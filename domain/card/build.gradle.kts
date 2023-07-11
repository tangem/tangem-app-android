plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    implementation(projects.domain.core)
// [REDACTED_TODO_COMMENT]
    implementation(projects.domain.models)

    implementation(projects.core.analytics.models)

    implementation(deps.tangem.card.core)
}
