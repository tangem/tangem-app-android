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
    /** Project - Core */
    implementation(projects.core.analytics.models)

    /** Project - Domain */
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.staking.models)

    /** SDK dependencies */
    implementation(deps.tangem.blockchain) {
        exclude(module = "joda-time")
    }

    /** Other dependencies */
    implementation(deps.kotlin.serialization)
    implementation(deps.jodatime)
    implementation(deps.timber)
}
