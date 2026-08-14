plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.foryou.impl"

    packaging {
        resources {
            merges += "paymentrequest.proto"
        }
    }
}

dependencies {

    /** Project - Features */
    api(projects.features.forYou.api)
    api(projects.features.promoBanners.api)
    implementation(projects.features.commonFeatures.api)

    /** Project - Core */
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    api(projects.core.configToggles)
    api(projects.core.decompose)
    api(projects.core.utils)
    implementation(projects.core.ui)

    /** Project - Common */
    api(projects.common.ui)
    implementation(projects.common.uiMarkets)
    implementation(projects.common.routing)

    /** Project - Domain */
    api(projects.domain.account.status)
    api(projects.domain.appCurrency)
    api(projects.domain.common)
    api(projects.domain.earn)
    api(projects.domain.markets)
    api(projects.domain.yieldSupply)
    api(projects.domain.staking)
    implementation(projects.domain.account)
    api(projects.domain.balanceHiding)
    api(projects.domain.wallets)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens)

    /** Project - Domain models */
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.markets.models)
    implementation(projects.core.pagination)
    implementation(projects.domain.staking.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.yieldSupply.models)
    implementation(projects.libs.crypto)
    implementation(projects.libs.blockchainSdk)

    /** Compose */
    api(deps.compose.animation)
    api(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)

    /** Other libraries */
    implementation(deps.androidx.appCompat)
    implementation(deps.arrow.core)
    implementation(deps.decompose.ext.compose)
    implementation(deps.decompose)
    implementation(deps.haze)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization)
    implementation(deps.lifecycle.compose)

    /** DI */
    implementation(deps.hilt.android)
    implementation(deps.androidx.annotation)
    implementation(deps.jodatime)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
    testImplementation(projects.test.mock)
    testImplementation(deps.androidx.annotation)
    testImplementation(tangemDeps.blockchain)
    testImplementation(tangemDeps.card.core)
}