plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.data.addressbook"
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    implementation(deps.kotlin.serialization)
    // endregion

    // region SDK
    implementation(deps.androidx.datastore)
    implementation(deps.arrow.core)
    implementation(deps.jodatime)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Project - Core
    api(projects.core.datasource)
    api(projects.core.utils)
    // endregion

    // region Project - Data
    api(projects.data.common)
    // endregion

    // region Project - Domain
    api(projects.domain.addressBook)
    api(projects.domain.common)
    api(projects.domain.models)
    // endregion

    // region Testing
    testImplementation(projects.test.core)
    // endregion
}