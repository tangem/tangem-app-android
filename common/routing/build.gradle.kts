plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.common.routing"
}

dependencies {
    /* Core */
    implementation(projects.core.decompose)

    /* Domain */
    implementation(projects.domain.qrScanning.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.staking.models)

    /* Libs - Other */
    api(deps.kotlin.serialization)
    implementation(deps.androidx.core.ktx)
    implementation(deps.timber)
}
