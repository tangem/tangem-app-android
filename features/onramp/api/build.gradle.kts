plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("configuration")
}

android {
    namespace = "com.tangem.features.onramp.api"
}

dependencies {
    /* Kotlin */
    api(deps.kotlin.coroutines)

    /* Project - Core */
    api(projects.core.decompose)
    api(projects.core.ui)

    /* Project - Domain */
    api(projects.domain.models)
    api(projects.domain.onramp.models)
}