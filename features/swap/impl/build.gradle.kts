plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.swap.presentation"
}
dependencies {
    /** Api */
    api(projects.features.approval.api)
    api(projects.features.commonFeatures.api)
    api(projects.features.send.api)
    api(projects.features.swap.api)
    api(projects.features.swap.domain)
    implementation(projects.features.send.impl)

    /** Core modules */
    api(projects.core.analytics)
    api(projects.core.analytics.models)
    api(projects.core.configToggles)
    api(projects.core.datasource)
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.ui)
    api(projects.core.utils)
    implementation(projects.core.error)
    api(projects.common.routing)
    api(projects.common.ui)

    /** Data modules */
    implementation(projects.data.common)

    /** Domain modules **/
    api(projects.domain.account.status)
    api(projects.domain.appCurrency)
    api(projects.domain.appCurrency.models)
    api(projects.domain.balanceHiding)
    api(projects.domain.card)
    api(projects.domain.express.models)
    api(projects.domain.feedback)
    api(projects.domain.legacy)
    api(projects.domain.models)
    api(projects.domain.settings)
    api(projects.domain.stories)
    api(projects.domain.swap)
    api(projects.domain.swap.models)
    api(projects.domain.tokens)
    api(projects.domain.transaction)
    api(projects.domain.transaction.models)
    api(projects.domain.txhistory)
    api(projects.domain.visa)
    api(projects.domain.wallets)
    implementation(projects.domain.account)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.core)
    implementation(projects.domain.feedback.models)
    implementation(projects.domain.markets.models)
    implementation(projects.domain.stories.models)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.visa.models)
    runtimeOnly(projects.domain.staking)

    /** AndroidX */
    implementation(deps.androidx.activity.compose)
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)
    implementation(deps.lifecycle.compose)
    implementation(deps.lifecycle.runtime.ktx)

    /** Compose */
    api(deps.compose.coil)
    api(deps.compose.foundation)
    api(deps.decompose.ext.compose)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.constraintLayout)

    /** Libs */
    implementation(projects.libs.blockchainSdk)
    implementation(projects.libs.crypto)

    /** Common */
    implementation(projects.common)

    /** Other libraries */
    api(deps.arrow.core)
    api(deps.kotlin.coroutines)
    api(deps.kotlin.immutable.collections)
    implementation(deps.jodatime)
    implementation(deps.kotlin.serialization)
    implementation(deps.firebase.perf) {
        exclude(group = "com.google.firebase", module = "protolite-well-known-types")
        exclude(group = "com.google.protobuf", module = "protobuf-javalite")
    }

    /** Tangem libs */
    api(tangemDeps.blockchain)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Test */
    testImplementation(projects.domain.core)
    testImplementation(projects.domain.legacy)
    testImplementation(projects.libs.blockchainSdk)
    testImplementation(projects.test.core)
}