plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.data.search"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.androidx.datastore)
    api(deps.moshi)
    ksp(deps.moshi.kotlin.codegen)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Project - Core
    api(projects.core.utils)
    implementation(projects.core.datasource)
    // endregion

    // region Project - Domain
    api(projects.domain.account.status)
    api(projects.domain.common)
    api(projects.domain.search)
    implementation(projects.domain.models)
    // endregion
}