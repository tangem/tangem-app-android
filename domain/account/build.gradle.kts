plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {

    api(projects.domain.core)
    api(projects.domain.models)
    api(projects.domain.wallets.models)

    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.serialization)

    testImplementation(projects.test.core)
    testRuntimeOnly(deps.test.junit5.engine)
}