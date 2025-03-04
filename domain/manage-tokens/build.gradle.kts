plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.managetokens"
}

dependencies {

    /* Domain */
    api(projects.domain.manageTokens.models)
    api(projects.domain.core)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.staking)
    implementation(projects.domain.tokens)
    implementation(projects.domain.card)
    implementation(projects.domain.legacy)

    /* Core */
    api(projects.core.pagination)
}