plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

dependencies {
    implementation(deps.tangem.card.core)
    implementation(deps.tangem.blockchain)

    /** DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)

    /** Coroutines */
    implementation(deps.kotlin.coroutines)
}
