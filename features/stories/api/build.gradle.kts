plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("configuration")
}

android {
    namespace = "com.tangem.features.stories.api"
}

dependencies {
    /* Project - Core */
    api(projects.core.decompose)
    api(projects.core.ui)

    /* Compose */
    api(deps.kotlin.immutable.collections)
    api(projects.common.routing)
}