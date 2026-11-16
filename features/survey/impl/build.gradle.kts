plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.features.survey.impl"
}
dependencies {
    /* Project - API */
    implementation(projects.features.survey.api)

    /* Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)

    /* Core */
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.configToggles)
    implementation(projects.core.local)
    implementation(projects.core.remote)
    implementation(projects.store.datasource)
    implementation(projects.core.decompose)
    implementation(projects.core.navigation)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /* Common */
    implementation(projects.common.routing)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /* Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.runtime)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.lifecycle.compose)

    /* Other */
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.serialization.core)
    implementation(deps.moshi)
    implementation(deps.retrofit)
    implementation(deps.arrow.core)
    implementation(deps.androidx.datastore.core)

    /** Tangem libraries */
    implementation(tangemDeps.card.core)
    implementation(deps.surveysparrow)
    implementation(deps.androidx.appCompat)
    implementation(deps.decompose)

    /** Tests */
    testImplementation(projects.test.core)
}