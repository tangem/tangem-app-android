plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.tokensync"
}

dependencies {
    api(projects.domain.core)
    implementation(projects.domain.models)
    implementation(projects.domain.account.status)
    implementation(projects.core.utils)

    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)
}