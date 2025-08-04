plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.express.models"
}

dependencies {
    /** Core */
    implementation(projects.core.utils)
    implementation(projects.core.datasource)

    /** Domain */
    implementation(projects.domain.express.models)
    api(projects.domain.models)
    implementation(projects.domain.tokens.models)

    /** Other */
    implementation(deps.moshi.adapters)
}