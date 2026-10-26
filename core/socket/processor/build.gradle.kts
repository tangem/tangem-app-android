plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    implementation(deps.ksp.api)
    implementation(deps.gradle.kotlinpoet)
    implementation(deps.kotlinpoet.ksp)

    testImplementation(projects.core.socket)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.kctfork.core)
    testImplementation(deps.test.kctfork.ksp)
}