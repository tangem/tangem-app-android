plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.notifications"
}

dependencies {
    implementation(projects.core.utils)
    implementation(projects.domain.core)
    implementation(projects.domain.models)
    implementation(projects.domain.notifications.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.libs.crypto)

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // end

    // region Tests
    testImplementation(deps.test.junit)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.mockk)
    // end
}