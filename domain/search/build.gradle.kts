plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.search"
}

dependencies {
    api(projects.domain.core)
    api(projects.domain.models)
    api(deps.kotlin.coroutines)
    implementation(projects.domain.common)
    implementation(projects.domain.account)
    implementation(projects.domain.account.status)

    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
    testImplementation(deps.arrow.core)
    testImplementation(deps.kotlin.coroutines)
}