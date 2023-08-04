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
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    implementation(deps.tangem.card.android)
    implementation(deps.tangem.card.core)

    implementation(projects.core.utils)

    implementation(projects.data.source.preferences)

    implementation(projects.domain.card)
    implementation(projects.domain.models)
}