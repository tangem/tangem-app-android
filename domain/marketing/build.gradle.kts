plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    implementation(projects.domain.marketing.models)
    api(projects.domain.models)

    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)

    testImplementation(projects.test.core)
}