plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.swap.v2.api"
}

dependencies {
    /** Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

    /** Common */
    implementation(projects.common.ui)

    /** Domain */
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.express.models)
    implementation(projects.domain.swap.models)
    implementation(projects.domain.manageTokens.models)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.appCurrency.models)

    /** Compose */
    implementation(deps.compose.runtime)

    /** Other */
    implementation(deps.kotlin.immutable.collections)
}