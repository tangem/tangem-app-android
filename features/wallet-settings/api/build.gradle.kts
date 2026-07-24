plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.walletsettings.api"
}

dependencies {

    /* Project - Domain */
    api(projects.domain.models)

    /* Project - Core */
    api(projects.core.decompose)
    api(projects.core.ui)
}