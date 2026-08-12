plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.staking.impl"
}
dependencies {
    /** AndroidX */
    implementation(deps.androidx.activity.compose)
    implementation(deps.androidx.appCompat)

    /** Other dependencies */
    implementation(deps.kotlin.datetime)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.material)
    implementation(deps.arrow.core)
    implementation(deps.lifecycle.compose)
    implementation(deps.moshi)

    /** Compose */
    implementation(deps.compose.material3)
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)

    /** Tangem SDKs */
    implementation(tangemDeps.card.core)
    implementation(tangemDeps.blockchain)

    /** Core modules */
    implementation(projects.core.ui)
    implementation(projects.core.utils)
    implementation(projects.core.navigation)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.decompose)

    /** Domain */
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.staking)
    implementation(projects.domain.balanceHiding)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.legacy)
    implementation(projects.domain.models)
    implementation(projects.domain.transaction)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.txhistory)
    implementation(projects.domain.feedback)
    implementation(projects.domain.feedback.models)
    implementation(projects.domain.account.status)
    implementation(projects.domain.marketing.models)
    implementation(projects.domain.onramp.models)

    /** Common */
    implementation(projects.common.ui)
    implementation(projects.common.routing)

    /** Libs */
    implementation(projects.libs.crypto)

    /** Feature modules */
    implementation(projects.features.staking.api)
    implementation(projects.features.txhistory.api)
    implementation(projects.features.approval.api)
    implementation(projects.features.marketing.api)
    implementation(projects.common)
    implementation(projects.domain.core)
    implementation(projects.domain.onramp.models)

    /** Decompose */
    implementation(deps.decompose.ext.compose)

    /** DI */
    implementation(deps.hilt.android)
    implementation(deps.androidx.annotation)
    implementation(deps.kotlin.serialization.core)
    kapt(deps.hilt.kapt)

    /** Test */
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.kotlin.coroutines)
    testImplementation(deps.jodatime)
    testImplementation(projects.domain.txhistory.models)
    api(deps.kotlin.coroutines)
    api(projects.domain.staking.models)
    api(projects.core.configToggles)
}