plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.markets"
}


dependencies {
    api(projects.domain.appCurrency.models)
    api(projects.domain.core)
    api(projects.core.pagination)
    api(projects.domain.markets.models)

    implementation(deps.kotlin.serialization)
    implementation(projects.domain.tokens.models)
}
