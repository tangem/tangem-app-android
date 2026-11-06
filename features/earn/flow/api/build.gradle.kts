plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.earn.flow.api"
}

dependencies {
    /** Feature */
    api(projects.features.earn.api)
    api(projects.features.earn.product.api)

    /** Core */
    api(projects.core.decompose)
    api(projects.core.ui)

    /** Domain */
    api(projects.domain.models)
}