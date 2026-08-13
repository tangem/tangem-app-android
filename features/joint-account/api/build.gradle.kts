plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.jointaccount.api"
}

dependencies {
    /** Core */
    api(projects.core.decompose)
    api(projects.core.ui)

    /** Domain */
    api(projects.domain.models)

    /** Common */
    api(projects.common.ui)

    /** Compose */
    api(deps.compose.foundation)
    implementation(deps.compose.runtime)
    implementation(deps.compose.ui)

    /** Other */
    api(deps.kotlin.immutable.collections)
}