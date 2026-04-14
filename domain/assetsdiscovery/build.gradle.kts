plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.assetsdiscovery"
}

dependencies {
    api(projects.domain.core)
    implementation(projects.domain.models)
    implementation(projects.domain.account.status)
    implementation(projects.core.utils)

    implementation(projects.libs.blockchainSdk)
    implementation(tangemDeps.blockchain)

    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)
}