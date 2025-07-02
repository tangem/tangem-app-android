plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.send.v2.api"
}

dependencies {
    /** Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

    /** Common */
    implementation(projects.common.ui)

    /** Domain models */
    api(projects.domain.models)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.nft.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.wallets.models)

    /** Compose */
    implementation(deps.compose.runtime)
    implementation(deps.compose.foundation)

    /** Tangem */
    implementation(tangemDeps.blockchain)

    /** Other */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.immutable.collections)
}