plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.feed.earn.impl"
}

dependencies {
    /* Project - API */
    implementation(projects.features.feed.api)
    implementation(projects.features.promoBanners.api)
    implementation(projects.features.commonFeatures.api)

    /* Project - Domain */
    implementation(projects.domain.earn)
    implementation(projects.domain.markets)
    implementation(projects.domain.models)
    implementation(projects.domain.markets.models)
    implementation(projects.domain.tokens.models)

    /* Project - Core */
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.decompose)
    implementation(projects.core.pagination)
    implementation(projects.core.remote)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /* Project - Common */
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /* Project - Libs */
    implementation(projects.libs.blockchainSdk)
    implementation(tangemDeps.blockchain)

    /* Compose */
    implementation(deps.compose.coil)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.ui.utils)
    implementation(deps.lifecycle.compose)

    /* Other */
    implementation(deps.androidx.appCompat)
    implementation(deps.arrow.core)
    implementation(deps.decompose)
    implementation(deps.decompose.ext.compose)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(projects.test.core)
}