plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    implementation(projects.domain.core)
    implementation(projects.domain.wallets.models)
    implementation(projects.core.utils)
}
