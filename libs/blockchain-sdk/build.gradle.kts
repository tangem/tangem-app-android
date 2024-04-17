plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.libs.blockchain_sdk"
}

dependencies {

    // region Core modules
    implementation(projects.core.datasource)
    implementation(projects.core.utils)
    // endregion

    // region AndroidX libraries
    implementation(deps.androidx.datastore)
    // endregion

    // region DI libraries
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
    // endregion

    // region Other libraries
    implementation(deps.kotlin.coroutines)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.timber)
    // endregion

    // region Tangem libraries
    implementation(deps.tangem.blockchain) { exclude(module = "joda-time") }
    implementation(deps.tangem.card.core)
    // endregion
}