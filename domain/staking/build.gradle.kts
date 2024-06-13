plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.staking"
}


dependencies {
    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)
    implementation(deps.kotlin.serialization)


    implementation(projects.domain.tokens.models)
}