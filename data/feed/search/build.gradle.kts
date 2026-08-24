plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.data.feed.search"
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    implementation(deps.kotlin.serialization)
    // endregion

    // region SDK
    implementation(deps.androidx.datastore)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Project - Core
    implementation(projects.core.local)
    implementation(projects.core.utils)
    // endregion

    // region Project - Domain
    api(projects.domain.feed.search)
    // endregion

    // region Testing
    testImplementation(projects.test.core)
    // endregion
}