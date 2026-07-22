plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("configuration")
}

android {
    namespace = "com.tangem.features.tokenrecieve.api"
}

dependencies {
    /** Core */
    api(projects.core.decompose)
    api(projects.core.ui)

    /** Domain */
    api(projects.domain.models)

    /** Runtime */
    runtimeOnly(deps.room.runtime)
}