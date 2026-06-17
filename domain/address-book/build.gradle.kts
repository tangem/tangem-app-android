plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.addressbook"
}

dependencies {

    api(projects.domain.core)
    api(projects.domain.models)

    implementation(projects.domain.transaction)
    implementation(projects.domain.tokens)

    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.serialization)
    implementation(deps.jodatime)

    // region Test libraries
    testImplementation(projects.test.core)
    testImplementation(projects.test.mock)
    testImplementation(projects.common.test)
    // endregion
}