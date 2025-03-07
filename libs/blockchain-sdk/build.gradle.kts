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

    // region Core modules
    implementation(projects.core.datasource)
    implementation(projects.core.configToggles)
    implementation(projects.core.utils)
    // endregion

    // region AndroidX libraries
    implementation(deps.androidx.datastore)
    // endregion

    // region DI libraries
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Other libraries
    implementation(deps.kotlin.coroutines)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.timber)
    ksp(deps.moshi.kotlin.codegen)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)
    // endregion

    // region Firebase libraries
    implementation(platform(deps.firebase.bom))
    implementation(deps.firebase.analytics)
    implementation(deps.firebase.crashlytics)
    // endregion

    // region Tangem libraries
    implementation(tangemDeps.blockchain) { exclude(module = "joda-time") }
    implementation(tangemDeps.card.core)
    // endregion

    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
}