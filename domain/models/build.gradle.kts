plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {
    implementation(deps.tangem.card.core)
    implementation(deps.moshi.kotlin)
    implementation(deps.kotlin.serialization)
}