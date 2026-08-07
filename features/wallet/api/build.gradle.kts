plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.wallet.api"
}

dependencies {
    /** Project - Domain */
    api(projects.domain.models)
    api(projects.domain.visa.models)

    /** Tangem libraries */
    api(tangemDeps.card.core)

    /** Core */
    api(projects.core.ui)
    api(projects.core.decompose)
    api(projects.common.ui)

    /** Other */
    api(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)
}