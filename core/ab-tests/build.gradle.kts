plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.core.abtests"

    // `src/prodDi/` holds the production binding of the manager; `mocked` supplies its own from `src/mocked/`
    buildTypes.configureEach {
        if (name != "mocked") {
            sourceSets.named(name) {
                java.srcDir("src/prodDi/kotlin")
            }
        }
    }
}

dependencies {

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    implementation(deps.amplitude.experiment)
    // endregion

    // region Core modules
    implementation(projects.core.analytics.models)
    implementation(projects.core.utils)
    // endregion
}