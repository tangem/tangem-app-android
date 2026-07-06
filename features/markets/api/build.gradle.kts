plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    id("configuration")
}

android {
    namespace = "com.tangem.features.markets.api"
}

dependencies {
    api(deps.kotlin.serialization.core)

    /* Project - Core */
    api(projects.core.decompose)
    api(projects.core.ui)

    /* Project - Domain */
    api(projects.domain.models)
}