plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.nft"
}

dependencies {
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)

    implementation(projects.domain.models)

    implementation(tangemDeps.blockchain)
}