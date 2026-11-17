plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.features.earn.product.api"
}

dependencies {
    /** Feature */
    api(projects.features.earn.api)

    /** Common */
    api(projects.common)

    /** Core */
    api(projects.core.analytics.models)
    api(projects.core.decompose)
    implementation(projects.core.res)
    api(projects.core.ui)
    api(projects.core.utils)

    /** Domain */
    api(projects.domain.models)
    api(projects.domain.transaction)
    api(projects.domain.transaction.models)

    /** SDK */
    api(tangemDeps.blockchain)

    /** Kotlin */
    api(deps.arrow.core)
    api(deps.kotlin.coroutines)
    api(deps.kotlin.datetime)
    api(deps.kotlin.serialization.core)

    /** DI */
    implementation(deps.hilt.core)

    /** Tests */
    testImplementation(projects.test.core)
    testImplementation(deps.kotlin.serialization)
}