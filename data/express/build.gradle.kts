plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.data.express"
}

dependencies {

    /** Core */
    implementation(projects.core.datasource)
    implementation(projects.core.utils)

    /** Data */
    implementation(projects.data.common)

    /** Domain */
    implementation(projects.domain.express.models)
    implementation(projects.domain.express)
    implementation(projects.domain.wallets.models)

    /** Other */
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.timber)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}