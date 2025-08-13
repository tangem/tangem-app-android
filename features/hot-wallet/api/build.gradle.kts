plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.hotwallet.api"
}

dependencies {

    /* Project - Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)

    /* Project - Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

    /* Tangem libraries */
    implementation(tangemDeps.card.core)

    /* Compose */
    implementation(deps.compose.runtime)

    /* Tangem libs */
    implementation(tangemDeps.hot.core)
}