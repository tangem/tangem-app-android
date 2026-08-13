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
    implementation(deps.kotlin.coroutines)

    api(deps.moshi)
    ksp(deps.moshi.kotlin.codegen)
    api(deps.retrofit)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    api(projects.core.remote)
}