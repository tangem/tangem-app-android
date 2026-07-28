import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.libs.blockchain_sdk"
}

dependencies {

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region AndroidX
    implementation(deps.androidx.core)
    implementation(deps.androidx.datastore)
    // endregion

    // region Other libraries
    implementation(deps.moshi)
    ksp(deps.moshi.kotlin.codegen)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)
    // endregion

    // region Firebase
    implementation(platform(deps.firebase.bom))
    implementation(deps.firebase.analytics)
    implementation(deps.firebase.crashlytics)
    // endregion

    // region Tangem
    api(tangemDeps.blockchain) { exclude(module = "joda-time") }
    implementation(tangemDeps.card.core)
    // endregion

    // region Core modules
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    api(projects.core.configToggles)
    api(projects.core.datasource)
    implementation(projects.core.utils)
    // endregion

    // region Domain models
    api(projects.domain.models)
    // endregion

    // region Tests
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    // endregion
}