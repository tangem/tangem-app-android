plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.assetsdiscovery"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    api(projects.domain.core)
    implementation(projects.domain.models)
    implementation(projects.domain.account.status)
    implementation(projects.core.utils)

    implementation(projects.libs.blockchainSdk)
    implementation(tangemDeps.blockchain)

    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)

    // region Tests
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
    // endregion
}