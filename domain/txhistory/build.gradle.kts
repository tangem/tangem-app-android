plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.txhistory"
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    implementation(deps.androidx.paging.runtime)
    // endregion

    // region Core modules
    api(projects.core.pagination)
    // endregion

    // region Domain models
    api(projects.domain.express.models)
    api(projects.domain.models)
    api(projects.domain.txhistory.models)
    // endregion
}