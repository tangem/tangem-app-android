plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.blockaid"
}


dependencies {

    // region Other libraries
    api(deps.arrow.core)
    api(tangemDeps.blockchain)
    // endregion

    // region Domain
    api(projects.domain.models)
    // endregion

    // region Domain models
    api(projects.domain.blockaid.models)
    // endregion
}