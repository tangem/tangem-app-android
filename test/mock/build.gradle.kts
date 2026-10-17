plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    api(projects.domain.account)
    api(projects.domain.models)
    api(projects.domain.yieldSupply.models)
    implementation(deps.arrow.core)
}