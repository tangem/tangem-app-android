plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    implementation(projects.domain.models)
    implementation(projects.domain.promo.models)
    implementation(projects.domain.settings)
    implementation(projects.domain.wallets.models)

    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)
}