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
    namespace = "com.tangem.core.configtoggle"
}

dependencies {
    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Local storages */
    implementation(deps.androidx.datastore)

    /** Other libraries */
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.timber)
    ksp(deps.moshi.kotlin.codegen)

    /** Core modules */
    implementation(projects.core.datasource)
    implementation(projects.core.utils)

    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
}