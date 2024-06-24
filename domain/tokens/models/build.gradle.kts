plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.tokens.model"
}

dependencies {
    implementation(deps.kotlin.serialization)
    implementation(projects.domain.txhistory.models)
    implementation(projects.core.analytics.models)
    implementation(deps.tangem.blockchain) {
        exclude(module = "joda-time")
    }
    implementation(deps.jodatime)
    implementation(deps.timber)
}