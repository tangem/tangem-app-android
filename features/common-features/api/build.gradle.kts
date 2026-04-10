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
    /** Api */ // todo swap delete after move portfolio selector
    implementation(projects.features.account.api)

    /* Project - Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.markets)

    /* Project - Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

    /* Compose */
    implementation(deps.compose.runtime)
    implementation(deps.kotlin.immutable.collections)
}