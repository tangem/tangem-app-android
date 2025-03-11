plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.wallet"
}

dependencies {

    /** Tangem libraries */
    implementation(tangemDeps.blockchain) // android-library

    /** Core */
    implementation(projects.core.datasource)
    implementation(projects.core.utils)

    /** Domain */
    implementation(projects.domain.wallets)

    /** Domain models */
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.tokens.models)

    /** DI */
    implementation(deps.hilt.android)
    implementation(project(":domain:legacy"))
    kapt(deps.hilt.kapt)

    /** Other deps */
    implementation(deps.androidx.datastore)
    implementation(deps.arrow.core)
}