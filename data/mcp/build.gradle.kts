plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.mcp"
}

dependencies {
    api(projects.domain.mcp)

    implementation(projects.core.remote)
    implementation(projects.domain.common)
    implementation(projects.core.utils)
    implementation(projects.domain.models)
    implementation(projects.libs.auth)

    implementation(deps.arrow.core)
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    implementation(deps.kotlin.coroutines)
    implementation(deps.retrofit)

    testImplementation(projects.test.core)
}