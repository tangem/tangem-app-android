plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.hotwallet.api"
}

dependencies {
    api(projects.common.routing)

    /* Project - Domain */
    api(projects.domain.models)
    api(projects.domain.wallets)

    /* Project - Core */
    api(projects.core.analytics.models)
    api(projects.core.decompose)
    api(projects.core.ui)

    /* Tangem libraries */
    api(tangemDeps.card.core)
}