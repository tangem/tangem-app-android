plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.earn.staking.impl"
}

dependencies {
    /** Feature */
    implementation(projects.features.earn.api)
    implementation(projects.features.earn.product.api)

    /** Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** Domain */
    implementation(projects.domain.account)
    implementation(projects.domain.account.status)
    implementation(projects.domain.models)
    implementation(projects.domain.staking)
    implementation(projects.domain.staking.models)

    /** Common */
    implementation(projects.common.routing)

    /** Kotlin */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(projects.test.core)
    testImplementation(projects.test.mock)
    testImplementation(projects.common.test)
    testImplementation(tangemDeps.blockchain)
}