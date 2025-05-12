plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {
    /** Project - Core */
    implementation(projects.core.analytics.models)

    /** Project - Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.staking.models)
    implementation(projects.domain.promo.models)

    /** Other dependencies */
    implementation(deps.kotlin.serialization)
    implementation(deps.jodatime)
}