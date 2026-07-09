plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.approval.api"
}

dependencies {

    // region Core
    api(projects.core.decompose)
    api(projects.core.ui)
    // endregion

    // region Common
    api(projects.common.ui)
    // endregion

    // region Domain
    api(projects.domain.models)
    // endregion
}