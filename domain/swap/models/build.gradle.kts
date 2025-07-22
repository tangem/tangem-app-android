plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.swap.models"
}

dependencies {
    /** Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.express.models)
    implementation(projects.domain.tokens.models)

    /** Core */
    implementation(projects.core.datasource)

    /** Other */
    implementation(deps.jodatime)
    implementation(deps.moshi)
}