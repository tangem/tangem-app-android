plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {

    api(projects.domain.models)
    api(projects.domain.wallets.models)

    implementation(deps.arrow.core)
    implementation(deps.kotlin.serialization)

    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.mockk)
}