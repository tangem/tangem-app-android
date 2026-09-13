plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.feed.search.api"
}

dependencies {

    /* Kotlin */
    api(deps.kotlin.coroutines)

    /* Compose */
    api(deps.compose.foundation)

    /* Project - Core */
    api(projects.core.decompose)
    api(projects.core.ui)

    /* Project - API */
    api(projects.features.feed.api)
}