plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.dynamicaddresses"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    api(projects.domain.core)
    api(projects.domain.dynamicAddresses.models)

    implementation(projects.domain.models)
    implementation(projects.domain.walletManager)
    implementation(projects.domain.wallets)
    implementation(projects.libs.blockchainSdk)

    implementation(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }
    implementation(tangemDeps.card.core)

    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
}