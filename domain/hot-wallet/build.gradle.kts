plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.hotwallet"
}

dependencies {
    implementation(projects.domain.core)
    implementation(projects.domain.models)
    implementation(projects.domain.wallets.models)

    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)

    testImplementation(deps.test.junit)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.mockk)
}