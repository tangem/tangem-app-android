plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.survey.impl"
}
dependencies {
    /* Project - API */
    implementation(projects.features.survey.api)

    /* Domain */
    implementation(projects.domain.common)
    implementation(projects.domain.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)

    /* Core */
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.configToggles)
    implementation(projects.core.datasource)
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /* Common */
    implementation(projects.common.routing)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /* Compose */
    implementation(deps.compose.runtime)
    implementation(deps.compose.ui)

    /* Other */
    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)

    /** Tangem libraries */
    implementation(tangemDeps.card.core)
    implementation(deps.surveysparrow)

    /** Tests */
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.coroutine)
}