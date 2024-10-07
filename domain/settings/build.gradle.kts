plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.wallets.models)
}