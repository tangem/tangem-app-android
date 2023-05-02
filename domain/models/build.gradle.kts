plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    implementation(deps.tangem.card.core)
    implementation(deps.moshi.kotlin)
}
