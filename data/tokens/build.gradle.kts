plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

dependencies {
    implementation(projects.domain.core)
    implementation(projects.domain.tokens)
    implementation(projects.domain.wallets.models)

    implementation(deps.kotlin.coroutines)

    implementation(deps.arrow.core)

    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
}