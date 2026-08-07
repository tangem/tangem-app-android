plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.kyc.api"
}

dependencies {

    /* Project - Core */
    api(projects.core.decompose)
    api(projects.core.ui)

    /* Project - Domain */
    api(projects.domain.models)
}