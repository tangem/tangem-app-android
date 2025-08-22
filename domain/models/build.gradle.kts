import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.ksp)
    id("configuration")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    api(projects.domain.visa.models)
    api(projects.core.utils)

    implementation(tangemDeps.card.core)
    implementation(tangemDeps.hot.core)
    implementation(deps.moshi.kotlin)
    implementation(deps.moshi.adapters)
    implementation(deps.kotlin.serialization)
    ksp(deps.moshi.kotlin.codegen)
    implementation(deps.arrow.core)

    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.mockk)
}