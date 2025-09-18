plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.account.status"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    implementation(projects.domain.account)
    implementation(projects.domain.core)
    implementation(projects.domain.quotes)
    implementation(projects.domain.models)
    implementation(projects.domain.networks)
    implementation(projects.domain.staking)
    implementation(projects.domain.tokens)

    implementation(projects.libs.crypto)

    implementation(deps.kotlin.datetime)

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // end

    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(tangemDeps.blockchain)
    testImplementation(projects.common.test)
}