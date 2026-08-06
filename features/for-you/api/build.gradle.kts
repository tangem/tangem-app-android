plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    id("configuration")
}

android {
    namespace = "com.tangem.features.foryou.api"
}

dependencies {
    /** Project - Core */
    api(projects.core.decompose)
    api(projects.core.ui)
    api(projects.core.utils)

    /** Project - Domain */
    api(projects.domain.models)
    api(projects.domain.markets.models)
}