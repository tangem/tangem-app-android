plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.walletconnect.api"
}

dependencies {
    /** Core */
    api(projects.core.decompose)
    api(projects.core.ui)
    api(projects.common.routing)

    /** Domain models */
    api(projects.domain.models)
}