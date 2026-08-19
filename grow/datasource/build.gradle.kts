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
    namespace = "com.tangem.grow.datasource"
}

dependencies {
    api(deps.moshi)
    ksp(deps.moshi.kotlin.codegen)
    api(deps.retrofit)
    api(deps.jodatime)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    implementation(tangemDeps.card.core)

    api(projects.core.remote)
    api(projects.core.utils)

    testImplementation(projects.test.core)
    testImplementation(deps.moshi.kotlin)
}