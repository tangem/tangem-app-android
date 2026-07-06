plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.visa"
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    api(deps.jodatime)
    implementation(deps.androidx.paging.runtime)
    runtimeOnly(deps.spongecastle.core)
    // endregion

    // region Core modules
    api(projects.core.analytics.models)
    api(projects.core.error)
    api(projects.core.pagination)
    api(projects.core.utils)
    // endregion

    // region Domain
    api(projects.domain.core)
    // endregion

    // region Domain models
    api(projects.domain.appCurrency.models)
    api(projects.domain.models)
    api(projects.domain.visa.models)
    // endregion

    // region Runtime
    runtimeOnly(deps.room.runtime)
    // endregion

    // region Tests
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(projects.test.core)
    // endregion
}