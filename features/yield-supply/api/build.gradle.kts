plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.yield.supply.api"
}

dependencies {
    /** Core */
    api(projects.core.decompose)
    api(projects.core.ui)
    api(projects.core.analytics.models)

    /** Domain */
    api(projects.domain.models)
    api(projects.domain.tokens.models)

    /** Compose */
}