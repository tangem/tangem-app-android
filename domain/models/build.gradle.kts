plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.ksp)
    id("configuration")
}
dependencies {

    // region Kotlin
    api(deps.kotlin.datetime)
    api(deps.kotlin.serialization)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    api(deps.jodatime)
    api(deps.moshi)
    implementation(deps.moshi.kotlin)
    ksp(deps.moshi.kotlin.codegen)
    // endregion

    // region Tangem SDK
    api(tangemDeps.card.core)
    api(tangemDeps.hot.core)
    // endregion

    // region Core modules
    // core:utils is intentionally re-exported (api): domain:models is a ubiquitous dependency and many
    // consumers rely on TangemLogger / utils through it. Demoting to implementation cascades across the
    // whole repo, so keep it api despite DAGP's incorrect-configuration advice (suppressed below).
    api(projects.core.utils)
    // endregion

    // region Domain
    api(projects.domain.core)
    // endregion

    // region Tests
    testImplementation(projects.test.core)
    // endregion
}