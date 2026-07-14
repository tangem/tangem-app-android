plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.send.api"
}
dependencies {
    /** Kotlin */
    api(deps.kotlin.coroutines)

    /** Core */
    api(projects.core.analytics.models)
    api(projects.core.decompose)
    api(projects.core.ui)
    implementation(projects.core.utils)

    /** Common */
    api(projects.common.ui)

    /** Domain */
    api(projects.domain.transaction)

    /** Domain models */
    api(projects.domain.appCurrency.models)
    api(projects.domain.models)
    api(projects.domain.nft.models)
    api(projects.domain.transaction.models)
    api(projects.domain.wallets.models)

    /** Compose */
    api(deps.compose.foundation)
    implementation(deps.compose.runtime)

    /** Tangem */
    api(tangemDeps.blockchain)

    /** Other */
    api(deps.kotlin.immutable.collections)
    implementation(deps.androidx.annotation)
    implementation(deps.arrow.core)

    // region Tests
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    // endregion
}