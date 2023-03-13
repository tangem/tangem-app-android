plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

dependencies {
    api(project(":domain:core"))

    implementation(deps.tangem.card.core)

    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
}
