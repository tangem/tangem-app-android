plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.features.commonfeatures.api"
}

dependencies {

    /* Project - Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.markets)
    implementation(projects.domain.account)

    /* Project - Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

    /* Compose */
    implementation(deps.compose.runtime)
    implementation(deps.kotlin.immutable.collections)
}