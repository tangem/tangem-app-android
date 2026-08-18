plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.promobanners.impl"
}

dependencies {
    /** Project - API */
    api(projects.features.promoBanners.api)
    implementation(projects.features.commonFeatures.api)
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /** Domain */
    api(projects.domain.common)
    implementation(projects.domain.models)
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.promo)
    implementation(projects.domain.promo.models)
    implementation(projects.domain.markets.models)

    /** Data */
    implementation(projects.data.common)

    /** Core */
    implementation(projects.core.local)
    api(projects.core.analytics)
    api(projects.core.datasource)
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.utils)
    api(projects.domain.wallets)
    api(projects.domain.account)
    implementation(projects.core.analytics.models)
    implementation(projects.core.ui)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.markets.models)
    implementation(projects.domain.wallets.models)

    /** Compose */
    api(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.material3)
    implementation(deps.lifecycle.compose)

    /** Other */
    implementation(deps.androidx.appCompat)
    implementation(deps.androidx.core.ktx)
    implementation(deps.decompose)
    implementation(deps.decompose.ext.compose)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)

    /** DI */
    implementation(deps.hilt.android)
    implementation(deps.arrow.core)
    implementation(deps.compose.material3)
    implementation(deps.haze)
    implementation(deps.kotlin.serialization.core)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(projects.test.core)
    testImplementation(projects.common.test)
    testImplementation(deps.arrow.core)
    testImplementation(tangemDeps.blockchain)
}