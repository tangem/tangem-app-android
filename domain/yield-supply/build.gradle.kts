plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.yield.supply"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    /** Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.transaction)
    implementation(projects.domain.legacy)

    /** Tandem SDK */
    implementation(tangemDeps.blockchain)

    /** Other */
    implementation(deps.arrow.core)

    /** tests */
    testImplementation(projects.common.test)
    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.mockk)
}