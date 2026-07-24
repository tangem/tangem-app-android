plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("configuration")
}

android {
    namespace = "com.tangem.features.staking.api"
}

dependencies {
    /** Kotlin */
    api(deps.kotlin.coroutines)

    /** Core */
    api(projects.core.decompose)
    api(projects.core.ui)

    /** Domain models */
    api(projects.domain.models)
    api(projects.domain.staking)
}