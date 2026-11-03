plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.earn.product.api"
}

dependencies {
    /** Core */
    api(projects.core.decompose)

    /** Domain */
    api(projects.domain.models)
}