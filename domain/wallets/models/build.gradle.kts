plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {
    // region Tangem libraries
    implementation(deps.tangem.card.core)
    // endregion

    // region Domain modules
    implementation(project(":domain:models"))
    // endregion

    // region Other libraries
    implementation(deps.kotlin.serialization)
    // endregion
}
