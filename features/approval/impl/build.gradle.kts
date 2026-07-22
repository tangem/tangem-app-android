plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.approval.impl"
}
dependencies {

    /** Feature */
    api(projects.features.approval.api)
    api(projects.features.send.api)

    /** Core */
    api(projects.core.analytics)
    api(projects.core.configToggles)
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)
    implementation(projects.core.ui)

    /** Common */
    implementation(projects.common)
    implementation(projects.common.ui)

    /** SDK */
    implementation(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }

    /** Domain */
    api(projects.domain.transaction)
    api(projects.domain.wallets)
    implementation(projects.domain.models)
    implementation(projects.domain.transaction.models)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.runtime)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)

    /** Other */
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)
    implementation(deps.arrow.core)
    implementation(deps.decompose)
    implementation(deps.decompose.ext.compose)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.lifecycle.compose)
    implementation(deps.lifecycle.runtime.ktx)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    // region Tests
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    // endregion
}