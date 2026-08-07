plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.tangempay.details.api"
}

dependencies {
    /** Core */
    api(projects.core.decompose)
    api(projects.core.ui)

    /** Domain */
    api(projects.domain.models)
    api(projects.domain.visa.models)
}