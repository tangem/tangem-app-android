import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.ksp)
    id("configuration")
}

dependencies {
    implementation(projects.domain.core)
    implementation(projects.domain.models)

    implementation(deps.kotlin.serialization)
    implementation(deps.jodatime)

    implementation(deps.moshi)
    ksp(deps.moshi.kotlin.codegen)
}