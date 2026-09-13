plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("configuration")
}

android {
    namespace = "com.tangem.features.tokendetails.api"
}

dependencies {
    /** Core */
    api(projects.core.decompose)
    api(projects.core.ui)

    /** Common */
    api(projects.common.ui)

    /** Domain models */
    api(projects.domain.models)
    api(deps.kotlin.coroutines)
    api(projects.domain.tokens.models)

    /** Compose */
    api(deps.compose.runtime)
    api(deps.compose.ui)
    api(deps.compose.foundation)

    /** Other */
    api(deps.kotlin.immutable.collections)
}