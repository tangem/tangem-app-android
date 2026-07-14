plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.nft.api"
}

dependencies {

    /* Project - Domain */
    api(projects.domain.models)
    api(projects.domain.nft.models)

    /* Project - Core */
    api(projects.core.decompose)
    api(projects.core.ui)
}