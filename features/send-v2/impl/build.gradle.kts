plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.send.v2.impl"
}

dependencies {
    /** Api */
    implementation(projects.features.sendV2.api)
    implementation(projects.features.txhistory.api)

    /** Libs */
    implementation(projects.libs.crypto)

    /** Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.analytics)
    implementation(projects.core.configToggles)
    implementation(projects.core.navigation)
    api(projects.core.pagination)

    /** Tangem SDK */
    implementation(tangemDeps.blockchain)

    /** Common */
    implementation(projects.common.ui)
    implementation(projects.common.routing)

    /** Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.legacy)
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
    implementation(projects.domain.txhistory)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.balanceHiding)

    /** Compose libraries */
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.material3)
    implementation(deps.decompose.ext.compose)
    implementation(deps.androidx.activity.compose)
    implementation(deps.androidx.paging.runtime)

    /** Other dependencies */
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.timber)
    implementation(deps.androidx.paging.runtime)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}