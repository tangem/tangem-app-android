plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.swap.domain.api"
}

dependencies {
    implementation(projects.features.swap.domain.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.express.models)

    implementation(deps.arrow.core)

}