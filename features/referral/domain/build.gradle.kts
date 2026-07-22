plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.referral"
}
dependencies {

    /** Core modules */
    api(projects.core.decompose)
    implementation(projects.core.utils)

    /** Domain modules */
    api(projects.domain.account)
    api(projects.domain.account.status)
    api(projects.domain.common)
    api(projects.domain.models)
    api(projects.domain.walletManager)
    implementation(projects.domain.core)

    /** Dependencies */
    api(deps.arrow.core)
    api(deps.jodatime)
    implementation(deps.kotlin.coroutines)
    implementation(tangemDeps.card.core)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}