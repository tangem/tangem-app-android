plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.visa"
}

dependencies {

    /** Project - Domain */
    implementation(projects.core.utils)
    implementation(projects.domain.core)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.appCurrency.models)

    /** Libs - Other */
    implementation(deps.jodatime)
    implementation(deps.androidx.paging.runtime)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
}