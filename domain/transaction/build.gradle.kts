plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.transaction"
}

dependencies {
    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)

    implementation(projects.core.utils)

    implementation(deps.tangem.blockchain)

    implementation(projects.domain.legacy)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
}