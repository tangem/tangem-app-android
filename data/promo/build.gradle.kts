plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.promo"
}

dependencies {
    implementation(deps.androidx.datastore)

    implementation(deps.jodatime)
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    implementation(projects.domain.promo)
    implementation(projects.domain.promo.models)
    implementation(projects.domain.wallets.models)

    implementation(projects.core.datasource)
    implementation(projects.core.utils)
}