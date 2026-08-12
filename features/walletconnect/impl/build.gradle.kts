plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.walletconnect.impl"
}

dependencies {
    implementation(projects.features.commonFeatures.api)
    implementation(projects.features.walletconnect.api)
    implementation(projects.features.send.api)

    /** Common */
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /** Core */
    implementation(projects.core.analytics)
    implementation(projects.core.decompose)
    implementation(projects.core.navigation)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** Domain models */
    implementation(projects.domain.account)
    implementation(projects.domain.account.status)
    implementation(projects.domain.blockaid.models)
    implementation(projects.domain.models)
    implementation(projects.domain.qrScanning.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.walletConnect)
    implementation(projects.domain.walletConnect.models)

    /** Domain */
    implementation(projects.domain.qrScanning)
    implementation(projects.domain.transaction)
    implementation(projects.domain.wallets)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** AndroidX */
    implementation(deps.androidx.activity.compose)

    /** Compose */
    implementation(deps.compose.coil)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)

    /** Data */
    implementation(projects.data.card)
    implementation(projects.common)
    implementation(projects.core.analytics.models)
    implementation(projects.domain.core)

    /** Other */
    implementation(deps.arrow.core)
    implementation(deps.decompose.ext.compose)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)
    implementation(deps.jodatime)
    implementation(deps.kotlin.serialization.core)
    implementation(deps.lifecycle.compose)
    implementation(deps.lifecycle.runtime.ktx)
    implementation(tangemDeps.card.core)
    implementation(tangemDeps.blockchain)

    /** Test libraries */
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.truth)
    api(deps.kotlin.coroutines)
}