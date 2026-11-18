plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.earn.impl"
}

dependencies {
    /** Feature */
    api(projects.features.earn.api)
    api(projects.features.earn.product.api)
    implementation(projects.features.earn.flow.api)
    implementation(projects.features.tokenRecieve.api)

    /** Common */
    implementation(projects.common)
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /** Core */
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.core.local)
    implementation(projects.core.media)
    implementation(projects.core.media.ui)
    implementation(projects.core.navigation)
    implementation(projects.core.res)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** Domain */
    implementation(projects.domain.account)
    implementation(projects.domain.account.status)
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.balanceHiding)
    implementation(projects.domain.common)
    implementation(projects.domain.models)
    implementation(projects.domain.onramp.models)
    implementation(projects.domain.quotes)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.transaction)
    implementation(projects.domain.transaction.models)

    /** SDK */
    implementation(projects.libs.crypto)
    implementation(tangemDeps.blockchain)

    /** Kotlin */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.datetime)
    implementation(deps.kotlin.serialization)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.runtime)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(tangemDeps.vico.core)
    implementation(tangemDeps.vico.compose)

    /** Other */
    implementation(deps.decompose)
    implementation(deps.decompose.ext.compose)
    implementation(deps.haze)
    implementation(deps.lifecycle.compose)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
    testImplementation(projects.test.mock)
}