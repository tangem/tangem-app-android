plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.virtualaccount"
}

dependencies {
    /** Project - Domain */
    api(projects.domain.models)
    api(projects.domain.virtualAccount.models)
    implementation(projects.domain.common)
    implementation(projects.domain.visa)

    /** Project - Core */
    implementation(projects.core.security)

    /** Coroutines */
    implementation(deps.kotlin.coroutines)

    /** Tests */
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.truth)
    testImplementation(projects.test.core)
    testImplementation(projects.common.test)
    testImplementation(projects.domain.card)
}