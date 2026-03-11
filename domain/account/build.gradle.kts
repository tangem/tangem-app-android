plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {

    api(projects.domain.common)
    api(projects.domain.core)
    api(projects.domain.models)
    api(projects.domain.wallets.models)
    api(projects.domain.yieldSupply.models)

    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.serialization)

    // region Test libraries
    testImplementation(projects.test.core)
    testImplementation(projects.test.mock)
    testRuntimeOnly(deps.test.junit5.engine)
    // endregion
}