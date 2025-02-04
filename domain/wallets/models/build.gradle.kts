plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
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
    implementation(deps.moshi.kotlin)
    implementation(deps.timber)
    kapt(deps.moshi.kotlin.codegen)
    // endregion
}