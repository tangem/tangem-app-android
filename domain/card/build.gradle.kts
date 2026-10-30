plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.card"
}

kotlin {
    compilerOptions {
        // ScanConfig.Callback members are defaults: JVM default methods keep implementors in other modules valid
        // when a default member is added, without compiled bridges
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}
dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    api(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }
    api(tangemDeps.card.core)
    // endregion

    // region Core modules
    api(projects.core.analytics.models)
    api(projects.core.utils)
    // core:error provides UniversalError — a supertype of VisaActivationError (used via
    // domain:visa:models) the compiler needs on the classpath, though never referenced directly.
    implementation(projects.core.error)
    // endregion

    // region Domain
    implementation(projects.domain.demo.models)
    // endregion

    // region Domain models
    api(projects.domain.models)
    api(projects.domain.visa.models)
    implementation(projects.domain.moneyAccount.models)
    // endregion

    // region Libs
    api(projects.libs.blockchainSdk)
    api(projects.libs.tangemSdkApi)
    // endregion

    // region Tests
    testImplementation(projects.test.core)
    // endregion
}