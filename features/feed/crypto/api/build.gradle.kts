plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.feed.crypto.api"
}

dependencies {
    /* Project - Core */
    api(projects.core.decompose)

    /* Project - API */
    api(projects.features.feed.api)
}