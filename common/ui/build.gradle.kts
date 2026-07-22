plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.common.ui"
}
dependencies {

    // region Kotlin
    api(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Compose
    api(deps.compose.foundation)
    api(deps.compose.material3)
    implementation(deps.compose.coil)
    implementation(deps.compose.constraintLayout)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    implementation(deps.hilt.android)
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.core.ktx)
    implementation(deps.haze)
    // endregion

    // region Tangem SDK
    api(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }
    // endregion

    // region Project - Core
    api(projects.core.analytics)
    api(projects.core.ui)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)
    implementation(projects.core.res)
    // endregion

    // region Project - Common
    // :common is intentionally re-exported (api): common:ui is a ubiquitous UI dependency and many
    // feature modules rely on TangemBlogUrlBuilder / common types through it. Demoting to implementation
    // cascades across the feature graph, so keep it api despite DAGP's advice (suppressed below).
    api(projects.common)
    // endregion

    // region Project - Domain
    api(projects.domain.account)
    api(projects.domain.appCurrency.models)
    api(projects.domain.common)
    api(projects.domain.core)
    api(projects.domain.models)
    api(projects.domain.onramp.models)
    api(projects.domain.staking)
    api(projects.domain.tokens.models)
    api(projects.domain.transaction.models)
    implementation(projects.domain.card)
    implementation(projects.domain.staking.models)
    // endregion

    // region Project - Libs
    implementation(projects.libs.blockchainSdk)
    implementation(projects.libs.crypto)
    // endregion

    // region Tests
    testImplementation(projects.test.core)
    // endregion
}