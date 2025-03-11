plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.appcurrency"
}

dependencies {

    /** Project - Domain */
    implementation(projects.domain.core)
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.appCurrency.models)

    /** Project - Data */
    implementation(projects.core.datasource)
    implementation(projects.data.common)

    /** Project - Utils */
    implementation(projects.core.utils)

    /** AndroidX */
    implementation(deps.androidx.datastore)

    /** DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)

    /** Other */
    implementation(deps.jodatime)
    implementation(deps.kotlin.coroutines)
    implementation(deps.timber)
}