plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.marketing.api"
}

dependencies {
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.domain.marketing.models)

    implementation(deps.kotlin.coroutines)
}