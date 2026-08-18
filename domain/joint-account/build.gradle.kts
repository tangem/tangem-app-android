plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {

    // region Tangem SDK (derivation path and derived public keys types)
    api(tangemDeps.card.core)
    // endregion

    // region Tests
    testImplementation(projects.test.core)
    testImplementation(deps.spongecastle.core)
    // endregion
}