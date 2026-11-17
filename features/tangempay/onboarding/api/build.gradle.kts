plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.tangempay.onboarding.api"
}

dependencies {
    /** Common */
    api(projects.common.routing)

    /** Core */
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.ui)

    /** Domain */
    api(projects.domain.models)

    /** Other */
    api(deps.kotlin.coroutines)
}