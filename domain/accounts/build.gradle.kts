plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {

    /* Projects - Domain */
    api(projects.domain.core)
    api(projects.domain.accounts.models)
    api(projects.domain.wallets.models)
}