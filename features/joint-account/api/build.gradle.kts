plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.jointaccount.api"
}

dependencies {
    /** Kotlin */
    api(deps.kotlin.coroutines)

    /** Core */
    api(projects.core.analytics.models)
    api(projects.core.decompose)
    implementation(projects.core.remote)
    api(projects.core.ui)

    /** Domain */
    api(projects.domain.models)

    /** Common */
    api(projects.common.routing)
    api(projects.common.ui)

    /** Compose */
    api(deps.compose.foundation)
    implementation(deps.compose.runtime)
    implementation(deps.compose.ui)

    /** Other */
    api(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization)

    /** Tests */
    testImplementation(projects.test.core)
}