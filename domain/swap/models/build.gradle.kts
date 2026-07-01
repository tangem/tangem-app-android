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

    // region Other libraries
    api(deps.jodatime)
    // endregion

    // region Core modules
    api(projects.core.datasource)
    // endregion

    // region Domain models
    api(projects.domain.express.models)
    api(projects.domain.models)
    // endregion
}