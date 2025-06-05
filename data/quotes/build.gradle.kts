plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.quotes"
}

dependencies {
    implementation(projects.core.datasource)
    implementation(projects.core.utils)

    implementation(projects.data.common)
    implementation(projects.data.tokens)

    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.quotes)
    implementation(projects.domain.wallets.models)

    implementation(deps.androidx.datastore)
    implementation(deps.moshi.kotlin)
    implementation(deps.timber)

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(projects.common.test)
}