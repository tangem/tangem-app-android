plugins {
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.android.library)
    id("configuration")
}

android {
    namespace = "com.tangem.data.blockaid"
}

dependencies {
    /* Project - Domain */
    implementation(projects.data.common)
    implementation(projects.domain.blockaid)
    implementation(projects.domain.blockaid.models)

    /* Project - Data */
    implementation(projects.core.datasource)

    /* Project - Core */
    implementation(projects.core.utils)

    /* DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)

    /* Tangem libraries */
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)

    /* Other */
    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)

    /* Tests */
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.turbine)
    testImplementation(deps.test.truth)
}