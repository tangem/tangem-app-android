plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.send.impl"
}
dependencies {
    /** Api */
    implementation(projects.features.send.api)
    implementation(projects.features.txhistory.api)
    implementation(projects.features.nft.api)
    implementation(projects.features.swapV2.api)
    implementation(projects.features.manageTokens.api)
    implementation(projects.features.addressBook.api)

    /** Libs */
    implementation(projects.libs.crypto)

    /** Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.analytics)
    implementation(projects.core.configToggles)
    implementation(projects.core.navigation)
    implementation(projects.core.datasource)
    implementation(projects.core.utils)

    /** Tangem SDK */
    implementation(tangemDeps.blockchain)

    /** Common */
    implementation(projects.common.ui)
    implementation(projects.common.routing)
    implementation(projects.common)

    /** Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.legacy)
    implementation(projects.domain.offramp)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.transaction)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.txhistory)
    implementation(projects.domain.qrScanning.models)
    implementation(projects.domain.qrScanning)
    implementation(projects.domain.settings)
    implementation(projects.domain.feedback)
    implementation(projects.domain.feedback.models)
    implementation(projects.domain.txhistory)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.balanceHiding)
    implementation(projects.domain.nft.models)
    implementation(projects.domain.nft)
    implementation(projects.domain.notifications)
    implementation(projects.domain.account)
    implementation(projects.domain.account.status)
    implementation(projects.domain.addressBook)
    implementation(projects.domain.transaction)
    implementation(projects.core.analytics.models)
    implementation(projects.domain.core)


    /** Compose libraries */
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.material3)
    implementation(deps.decompose.ext.compose)
    implementation(deps.androidx.activity.compose)

    /** Other dependencies */
    implementation(deps.kotlin.immutable.collections)

    /** DI */
    implementation(deps.hilt.android)
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)
    implementation(deps.arrow.core)
    implementation(deps.compose.reorderable)
    implementation(deps.jodatime)
    implementation(deps.kotlin.serialization.core)
    implementation(deps.lifecycle.compose)
    implementation(deps.lifecycle.runtime.ktx)
    kapt(deps.hilt.kapt)
    
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
    testImplementation(deps.arrow.core)
    testImplementation(deps.kotlin.coroutines)
    api(deps.kotlin.coroutines)
    api(projects.domain.quotes)
}