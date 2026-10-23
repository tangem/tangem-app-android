plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    api(projects.domain.account)
    api(projects.domain.models)
    api(projects.domain.staking.models)
    api(projects.domain.yieldSupply.models)
    api(deps.kotlin.datetime)
    implementation(deps.arrow.core)
    implementation(deps.jodatime)
}