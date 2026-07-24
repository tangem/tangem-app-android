plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.txhistory.impl"

    packaging {
        resources {
            merges += "paymentrequest.proto"
        }
    }
}
dependencies {
    /** Core */
    api(projects.core.configToggles)
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.ui)
    api(projects.core.utils)
    implementation(projects.core.pagination)

    /** Common */
    api(projects.common.ui)
    implementation(projects.common)

    /** Features api */
    api(projects.features.txhistory.api)
    implementation(projects.features.rating.api)

    /** Domain */
    api(projects.domain.account.status)
    api(projects.domain.balanceHiding)
    api(projects.domain.common)
    api(projects.domain.models)
    api(projects.domain.staking)
    api(projects.domain.txhistory)
    api(projects.domain.wallets)
    implementation(projects.domain.account)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.express.models)
    implementation(projects.domain.onramp.models)
    implementation(projects.domain.staking.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.txhistory.models)
    runtimeOnly(projects.domain.card)
    runtimeOnly(projects.domain.tokens)

    /** Compose */
    api(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.decompose.ext.compose)

    /** AndroidX */
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)
    implementation(deps.lifecycle.compose)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Other */
    implementation(deps.arrow.core)
    implementation(deps.jodatime)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)

    /** Test */
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
    testImplementation(projects.test.mock)
    testImplementation(projects.domain.express.models)
    testImplementation(deps.kotlin.coroutines)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.coroutine)
}