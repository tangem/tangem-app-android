plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    api(projects.domain.wallets.models)
    api(deps.kotlin.coroutines)

    testImplementation(projects.test.core)
}