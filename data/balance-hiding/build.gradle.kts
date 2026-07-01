plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.balancehiding"
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.lifecycle.runtime.ktx)
    implementation(deps.androidx.datastore)
    implementation(deps.moshi)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core modules
    api(projects.core.datasource)
    implementation(projects.core.utils)
    // endregion

    // region Domain
    api(projects.domain.balanceHiding)
    implementation(projects.domain.balanceHiding.models)
    // endregion
}
