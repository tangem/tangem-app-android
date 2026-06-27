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
    // region Project - Core
    implementation(projects.core.datasource)
    implementation(projects.core.utils)
    // endregion

    // region Project - Data
    implementation(projects.data.common)
    // endregion

    // region Project - Domain
    implementation(projects.domain.addressBook)
    implementation(projects.domain.common)
    implementation(projects.domain.models)
    // endregion

    // region SDK
    implementation(deps.androidx.datastore)
    implementation(deps.arrow.core)
    implementation(deps.jodatime)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.serialization)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Testing
    testImplementation(projects.test.core)
    testImplementation(deps.moshi.kotlin)
    // endregion
}