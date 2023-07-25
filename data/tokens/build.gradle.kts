plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.tokens"
}

dependencies {
    implementation(projects.domain.core)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.demo)
    implementation(projects.domain.wallets.models)

    implementation(projects.core.datasource)

    // FIXME: For blockchain extensions, remove after refactoring
    implementation(projects.domain.legacy)

    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)

    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)

    implementation(deps.timber)

    implementation(deps.tangem.blockchain)
    implementation(deps.tangem.card.core)
}