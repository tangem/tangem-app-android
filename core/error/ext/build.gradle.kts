plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.core.error.ext"
}

dependencies {
    api(projects.core.error)

    implementation(tangemDeps.card.core)
    implementation(tangemDeps.blockchain)
}