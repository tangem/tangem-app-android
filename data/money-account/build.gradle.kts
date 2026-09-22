plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.moneyaccount"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    implementation(deps.moshi)
    implementation(deps.androidx.datastore)
    implementation(deps.arrow.core)
    // endregion

    // region Libs - Tangem
    implementation(tangemDeps.card.core)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Project - Core
    implementation(projects.core.local)
    implementation(projects.core.remote)
    implementation(projects.core.security)
    implementation(projects.core.utils)
    // endregion

    // region Project - Datasource
    implementation(projects.spend.datasource)
    // endregion

    // region Project - Domain
    api(projects.domain.moneyAccount)
    implementation(projects.domain.common)
    implementation(projects.domain.core)
    implementation(projects.domain.models)
    implementation(projects.domain.networks)
    implementation(projects.domain.visa)
    implementation(projects.domain.wallets)
    // endregion

    // region Project - Domain models
    implementation(projects.domain.visa.models)
    // endregion

    // region Test
    testImplementation(projects.test.core)
    // endregion
}