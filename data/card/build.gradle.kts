plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.card"
}

dependencies {
    implementation(deps.androidx.datastore)

    implementation(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    implementation(tangemDeps.card.android)
    implementation(tangemDeps.card.core)

    implementation(projects.core.datasource)
    implementation(projects.core.utils)

    implementation(projects.libs.blockchainSdk)

    implementation(projects.domain.card)
    implementation(projects.domain.models)
}