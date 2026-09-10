plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    implementation(deps.spongecastle.core)
    // endregion

    // region Domain
    api(projects.domain.core)
    api(projects.core.utils)
    api(projects.domain.models)
    // endregion

    // region Tangem SDK (derivation path and derived public keys types)
    api(tangemDeps.card.core)
    // endregion

    // region Tests
    testImplementation(projects.test.core)
    testImplementation(projects.test.mock)
    testImplementation(deps.web3j.core)
    // endregion
}