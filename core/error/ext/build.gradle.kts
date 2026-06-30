plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.core.error.ext"
}

dependencies {

    // region Tangem
    api(tangemDeps.blockchain)
    api(tangemDeps.card.core)
    // endregion

    // region Core modules
    api(projects.core.error)
    // endregion
}