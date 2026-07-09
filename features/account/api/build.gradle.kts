plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.account.api"
}

dependencies {

    // region Project - Core
    api(projects.core.decompose)
    api(projects.core.ui)
    // endregion

    // region Project - Domain
    api(projects.domain.models)
    // endregion
}