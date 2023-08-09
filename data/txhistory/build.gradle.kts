plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.txhistory"
}

dependencies {
    implementation(projects.core.utils)
    implementation(projects.core.datasource)
    implementation(projects.domain.legacy)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.txhistory)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.wallets.models)

    implementation(deps.kotlin.coroutines)
    implementation(deps.androidx.paging.runtime)
    implementation(deps.arrow.core)

    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
}