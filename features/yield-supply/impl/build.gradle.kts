plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.yield.supply.impl"
}
dependencies {

    /** Feature */
    implementation(projects.features.yieldSupply.api)
    implementation(projects.features.marketing.api)

    /** Core */
    implementation(projects.core.datasource)
    implementation(projects.core.decompose)
    implementation(projects.core.res)
    implementation(projects.core.ui)
    implementation(projects.core.navigation)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.utils)

    /** Compose */
    implementation(tangemDeps.vico.core)
    implementation(tangemDeps.vico.compose)

    /** Common */
    implementation(projects.common.ui)
    implementation(projects.common.routing)

    /** SDK */
    implementation(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }

    /** Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.account.status)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.transaction)
    implementation(projects.domain.yieldSupply.models)
    implementation(projects.domain.yieldSupply)
    implementation(projects.domain.stories.models)
    implementation(projects.domain.stories)
    implementation(projects.domain.feedback.models)
    implementation(projects.domain.feedback)
    implementation(projects.domain.marketing.models)
    implementation(projects.libs.crypto)
    implementation(projects.common)
    implementation(projects.domain.account)
    implementation(projects.domain.onramp.models)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.runtime)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)

    /** Other */
    implementation(deps.decompose)
    implementation(deps.decompose.ext.compose)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.datetime)

    /** DI */
    implementation(deps.hilt.android)
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)
    implementation(deps.arrow.core)
    implementation(deps.kotlin.serialization.core)
    implementation(deps.lifecycle.compose)
    implementation(deps.lifecycle.runtime.ktx)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.arrow.core)
    testImplementation(deps.kotlin.coroutines)
    api(deps.kotlin.coroutines)
    api(projects.domain.networks)
    api(projects.core.configToggles)
}