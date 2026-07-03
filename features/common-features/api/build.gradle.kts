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

    // region Kotlin
    api(deps.kotlin.coroutines)
    api(deps.kotlin.immutable.collections)
    api(deps.kotlin.serialization.core)
    // endregion

    /* Project - Domain */
    api(projects.domain.account)
    api(projects.domain.appCurrency.models)
    api(projects.domain.markets.models)
    api(projects.domain.models)

    /* Project - Core */
    api(projects.core.decompose)
    api(projects.core.ui)

    /* Compose */
    implementation(deps.compose.runtime)
}