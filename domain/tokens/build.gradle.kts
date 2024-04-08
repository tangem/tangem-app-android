plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.tokens"
}

dependencies {

    /** Project - Domain */
    implementation(projects.domain.core)
    implementation(projects.domain.models)
    implementation(projects.domain.legacy)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.settings)
    implementation(projects.features.swap.domain.api)
    implementation(projects.features.swap.domain.models)
    implementation(projects.features.send.api)

    /** Project - Other */
    implementation(projects.core.utils)

    /** Android - Other */
    implementation(deps.androidx.paging.runtime)

    /** Utils */
    implementation(deps.jodatime)
    implementation(deps.reKotlin)

    /** Tests */
    testImplementation(deps.test.junit)
    testImplementation(deps.test.coroutine)
}
