plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {

    /* Domain */
    api(projects.domain.manageTokens.models)
    api(projects.domain.core)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.tokens.models)

    /* Core */
    api(projects.core.pagination)
}