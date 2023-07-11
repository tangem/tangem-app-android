plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    // region Tangem libraries
    implementation(deps.tangem.card.core)
    // endregion

    // region Domain modules
    implementation(project(":domain:models"))
    // endregion
}