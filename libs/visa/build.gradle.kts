import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.libs.visa"
}

dependencies {

    /** Project */
    implementation(projects.core.utils)
    implementation(projects.core.datasource)
    implementation(projects.data.common)

    /** Libs - Network */
    implementation(deps.moshi.kotlin)
    implementation(deps.okHttp)
    implementation(deps.okHttp.prettyLogging)
    implementation(deps.retrofit)
    implementation(deps.retrofit.moshi)
    kaptForObfuscatingVariants(deps.moshi.kotlin.codegen)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)

    /** Libs - Other */
    implementation(deps.web3j.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.fx)
    implementation(deps.jodatime)
}