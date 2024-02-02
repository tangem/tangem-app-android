plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.visa"
}

dependencies {

    /** Project - Domain */
    implementation(projects.domain.visa)
    implementation(projects.domain.wallets.models)

    /** DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
}