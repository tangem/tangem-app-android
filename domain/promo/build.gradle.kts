plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    implementation(projects.domain.promo.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.settings)

    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)
}