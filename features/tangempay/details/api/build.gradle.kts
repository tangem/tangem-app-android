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
    implementation(projects.core.utils)

    /** Domain */
    api(projects.domain.models)
    api(projects.domain.visa.models)

    /** Other */
    implementation(deps.androidx.annotation)
}