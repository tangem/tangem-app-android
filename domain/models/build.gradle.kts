import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

dependencies {
    api(projects.domain.visa.models)

    implementation(tangemDeps.card.core)
    implementation(deps.moshi.kotlin)
    implementation(deps.moshi.adapters)
    implementation(deps.kotlin.serialization)
    kapt(deps.moshi.kotlin.codegen)
}