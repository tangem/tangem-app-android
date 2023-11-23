plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

dependencies {

    /** Project*/
    implementation(project(":core:datasource"))
    implementation(project(":core:utils"))
    implementation(project(":features:swap:domain"))

    /** Network */
    implementation(deps.retrofit)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)

    /** Domain */
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.legacy)
    implementation(projects.domain.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)

    /** Data */
    implementation(projects.data.tokens)

    /** Tangem SDKs */
    implementation(deps.tangem.blockchain)

    /** DI */
    implementation(deps.hilt.android)

    kapt(deps.hilt.kapt)
}
