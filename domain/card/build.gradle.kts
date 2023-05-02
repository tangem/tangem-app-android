plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    implementation(project(":domain:core"))
    // TODO: Remove after new card scan result was implemented
    implementation(project(":domain:models"))

    implementation(deps.tangem.card.core)
}