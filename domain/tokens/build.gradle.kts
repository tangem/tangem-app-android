plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    implementation(project(":domain:core"))
    implementation(project(":domain:models"))
    implementation(project(":core:utils"))

    testImplementation(deps.test.junit)
    testImplementation(deps.test.coroutine)
}
