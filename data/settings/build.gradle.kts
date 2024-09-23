plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.settings"
}

dependencies {

    implementation(projects.core.datasource)
    implementation(projects.core.utils)

    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.settings)

    implementation(deps.androidx.datastore)

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Others dependencies
    implementation(deps.jodatime)
    implementation(deps.kotlin.coroutines)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.timber)
    // endregion
}
