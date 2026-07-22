plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.addressbook.api"
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    api(deps.kotlin.immutable.collections)
    // endregion

    // region Project - Common
    api(projects.common.routing)
    api(projects.common.ui)
    // endregion

    // region Project - Core
    api(projects.core.decompose)
    api(projects.core.ui)
    // endregion

    // region Project - Domain
    api(projects.domain.models)
    // endregion
}