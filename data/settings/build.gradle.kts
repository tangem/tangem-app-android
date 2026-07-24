import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.settings"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Others dependencies
    implementation(deps.androidx.core)
    implementation(deps.androidx.datastore)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core modules
    api(projects.core.datasource)
    api(projects.core.utils)
    // endregion

    // region Domain
    api(projects.domain.settings)
    // endregion

    // region Test
    testImplementation(projects.test.core)
    // endregion
}
