plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.card"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.androidx.fragment)
    implementation(deps.androidx.datastore)
    implementation(deps.moshi)
    // endregion

    // region Tangem SDK
    api(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }
    api(tangemDeps.card.core)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core modules
    api(projects.core.datasource)
    implementation(projects.core.utils)
    // endregion

    // region Domain
    api(projects.domain.card)
    api(projects.domain.models)
    // endregion

    // region Tests
    testImplementation(projects.test.core)
    // endregion
}