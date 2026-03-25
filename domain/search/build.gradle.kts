plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.search"
}

dependencies {
    api(projects.domain.core)
    api(projects.domain.models)
    implementation(projects.domain.common)
    implementation(projects.domain.markets.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.account)
    implementation(projects.domain.account.status)
}