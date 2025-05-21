plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.send.v2.api"
}

dependencies {
    /** Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

    /** Domain models */
    api(projects.domain.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.nft.models)

    /* Compose */
    implementation(deps.compose.runtime)
}
