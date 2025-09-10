plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    /** Core modules */
    implementation(projects.core.analytics.models)

    api(projects.domain.onramp.models)
    api(projects.domain.tokens.models)
    api(projects.domain.wallets.models)

    api(projects.domain.core)
    api(projects.domain.settings)
    implementation(deps.kotlin.serialization)

    /** Tests */
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
}