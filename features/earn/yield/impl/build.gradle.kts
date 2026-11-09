plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.earn.yield.impl"
}

dependencies {
    /** Feature */
    implementation(projects.features.earn.api)
    implementation(projects.features.earn.product.api)

    /** Core */
    implementation(projects.core.decompose)

    /** Domain */
    implementation(projects.domain.account)
    implementation(projects.domain.account.status)
    implementation(projects.domain.models)
    implementation(projects.domain.yieldSupply)

    /** Common */
    implementation(projects.common.routing)

    /** Libs */
    implementation(projects.libs.crypto)

    /** Kotlin */
    implementation(deps.kotlin.coroutines)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(projects.test.core)
    testImplementation(projects.common.test)
    testImplementation(projects.domain.staking)
    testImplementation(tangemDeps.blockchain)
}