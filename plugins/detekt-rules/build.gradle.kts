plugins {
    alias(deps.plugins.kotlin.jvm)
}

dependencies {
    api(deps.detekt.api)

    testImplementation(deps.detekt.test)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.truth)
}

// The module is a detekt plugin itself, so it can't apply the `configuration` convention plugin that wires JUnit 5
tasks.withType<Test> {
    useJUnitPlatform()
}