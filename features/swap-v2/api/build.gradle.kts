plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.swap.v2.api"
}

dependencies {
    /** Core */
    api(projects.core.decompose)
    api(projects.core.ui)

    api(projects.features.send.api)
    api(deps.kotlin.coroutines)

    /** Common */

    /** Domain */
    api(projects.domain.swap.models)
    api(projects.domain.manageTokens.models)
    api(projects.domain.models)

    /** Compose */

    /** Other */
}