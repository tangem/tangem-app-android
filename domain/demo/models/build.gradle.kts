plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.demo.models"
}

dependencies {
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)
}