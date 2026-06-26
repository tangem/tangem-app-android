plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.marketing"
}

dependencies {
    implementation(deps.androidx.datastore)

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)

    implementation(projects.domain.marketing)
    implementation(projects.domain.marketing.models)

    implementation(projects.core.datasource)
    implementation(projects.core.utils)
    implementation(projects.core.configToggles)

    testImplementation(projects.test.core)
}