plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.stories"
}

dependencies {
    implementation(deps.androidx.datastore)

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    implementation(projects.domain.stories)
    implementation(projects.domain.stories.models)
    api(projects.domain.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.features.referral.domain)

    implementation(projects.core.datasource)
    implementation(projects.core.utils)
}