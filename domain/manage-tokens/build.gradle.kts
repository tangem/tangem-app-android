plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.managetokens"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    runtimeOnly(deps.kotlin.coroutines.android)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    // endregion

    // region Core modules
    api(projects.core.pagination)
    api(projects.core.utils)
    // endregion

    // region Domain
    api(projects.domain.models)
    // endregion

    // region Domain models
    api(projects.domain.manageTokens.models)
    // endregion

    // region Runtime
    // room/coroutines-android reach the runtime classpath through transitive consumers that no longer
    // arrive via a compile dependency — declare them runtimeOnly so the runtime graph stays complete.
    runtimeOnly(deps.room.runtime)
    // endregion

    // region Tests
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.truth)
    // endregion
}