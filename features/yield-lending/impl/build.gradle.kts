plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.yieldlending.impl"
}

dependencies {

    /** Feature */
    implementation(projects.features.yieldLending.api)

    /** Core */
    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

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
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.transaction)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.runtime)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.androidx.activity.compose)

    /** Other */
    implementation(deps.decompose)
    implementation(deps.decompose.ext.compose)
    implementation(deps.timber)
    implementation(deps.kotlin.immutable.collections)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}