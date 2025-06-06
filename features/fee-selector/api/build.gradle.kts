plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.feeselector.api"
}

dependencies {
    /** Project - Core */
    implementation(projects.core.ui)
    implementation(projects.core.decompose)

    /** Domain models */
    // api(projects.domain.models)
    implementation(projects.domain.appCurrency.models)
    // implementation(projects.domain.wallets.models)

    /** Compose */
    implementation(deps.compose.runtime)
    implementation(deps.compose.foundation)

    /** Other */
    implementation(deps.kotlin.immutable.collections)
}