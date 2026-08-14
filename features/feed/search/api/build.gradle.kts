plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.features.feed.search.api"
}

dependencies {

    /* Kotlin */
    api(deps.kotlin.coroutines)
    api(deps.kotlin.serialization.core)

    /* Project - Core */
    api(projects.core.decompose)

    /* Project - API */
    api(projects.features.feed.api)
}