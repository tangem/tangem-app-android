import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.ksp)
    id("configuration")
}

dependencies {
    api(projects.domain.visa.models)
    api(projects.core.utils)

    implementation(tangemDeps.card.core)
    implementation(deps.moshi.kotlin)
    implementation(deps.moshi.adapters)
    implementation(deps.kotlin.serialization)
    ksp(deps.moshi.kotlin.codegen)
    implementation(deps.arrow.core)
}