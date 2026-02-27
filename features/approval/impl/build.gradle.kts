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
    implementation(projects.features.approval.api)
    implementation(projects.features.sendV2.api)

    /** Core */
    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.core.navigation)
    implementation(projects.core.ui)
    implementation(projects.core.utils)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)

    /** Common */
    implementation(projects.common.ui)

    /** SDK */
    implementation(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }

    /** Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.transaction)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.runtime)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)

    /** Other */
    implementation(deps.decompose)
    implementation(deps.decompose.ext.compose)
    implementation(deps.timber)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.arrow.core)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}