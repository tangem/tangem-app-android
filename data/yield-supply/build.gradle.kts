plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.yield.supply"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {

    /** Tangem SDKs */
    implementation(tangemDeps.blockchain)

    /** Core */
    implementation(projects.core.datasource)
    implementation(projects.core.utils)

    /** Domain */
    implementation(projects.domain.yieldSupply)
    implementation(projects.domain.walletManager)
    implementation(projects.domain.legacy)

    implementation(projects.libs.blockchainSdk)


    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Other */
    implementation(deps.timber)

    /** tests */
    testImplementation(projects.common.test)
    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.mockk)
}