plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.apptheme"
}

dependencies {

    /** Project - Domain */
    implementation(projects.domain.appTheme)
    implementation(projects.domain.appTheme.models)

    /** Project - Data */
    implementation(projects.core.datasource)

    /** Project - Utils */
    implementation(projects.core.utils)

    /** DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)

    /** Other */
    implementation(deps.kotlin.coroutines)

    /** Local storages */
    implementation(deps.androidx.datastore)
}