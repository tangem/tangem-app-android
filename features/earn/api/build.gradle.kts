plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.earn.api"
}

dependencies {
    /** Core */
    api(projects.core.decompose)
    api(projects.core.ui)

    /** Domain */
    api(projects.domain.core)
    api(projects.domain.models)

    /** Kotlin */
    api(deps.kotlin.coroutines)

    /** Tests */
    testImplementation(projects.test.core)
}