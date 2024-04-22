plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.demo"
}

dependencies {
    implementation(deps.tangem.blockchain)
    implementation(deps.tangem.card.core)
}