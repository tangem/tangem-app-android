plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.txhistory"
}

dependencies {
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.androidx.paging.runtime)

    implementation(projects.core.utils)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.txhistory.models)
}