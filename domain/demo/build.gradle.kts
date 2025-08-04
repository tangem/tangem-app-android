plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.demo"
}

dependencies {
    api(projects.domain.demo.models)

    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)
}