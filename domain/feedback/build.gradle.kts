plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.feedback"
}

dependencies {

    // region Other libraries
    api(deps.arrow.core)
    implementation(deps.jodatime)
    // endregion

    // region Core modules
    implementation(projects.core.res)
    // endregion

    // region Domain
    api(projects.domain.models)
    implementation(projects.domain.visa.models)
    // endregion

    // region Domain models
    api(projects.domain.feedback.models)
    // endregion

    // region Tests
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    // endregion
}