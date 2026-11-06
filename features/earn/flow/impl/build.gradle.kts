plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.earn.flow.impl"
}

dependencies {
    /** Feature */
    implementation(projects.features.earn.api)
    implementation(projects.features.earn.flow.api)
    implementation(projects.features.earn.product.api)
    implementation(projects.features.send.api)

    /** Common */
    implementation(projects.common.ui)

    /** Core */
    implementation(projects.core.decompose)
    implementation(projects.core.res)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** AndroidX */
    implementation(deps.androidx.appCompat)

    /** Domain */
    implementation(projects.domain.account)
    implementation(projects.domain.account.status)
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.feedback)
    implementation(projects.domain.feedback.models)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.transaction)
    implementation(projects.domain.transaction.models)

    /** SDK */
    implementation(tangemDeps.blockchain)

    /** Kotlin */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization.core)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.runtime)
    implementation(deps.compose.ui)
    implementation(deps.lifecycle.compose)

    /** Other */
    implementation(deps.decompose)
    implementation(deps.decompose.ext.compose)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(deps.kotlin.datetime)
    testImplementation(projects.test.core)
    testImplementation(projects.test.mock)
}