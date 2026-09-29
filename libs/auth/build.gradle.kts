plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.libs.auth"
}

// bcprov-jdk15to18 arrives transitively and ships the same classes as the bcprov declared below, so this
// module would compile against two copies while :app drops that artifact globally. Match :app so the
// classpath here is the one that actually ends up in the built app.
configurations.all {
    exclude(group = "org.bouncycastle", module = "bcprov-jdk15to18")
}

dependencies {

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Kotlin
    api(deps.kotlin.datetime)
    api(deps.kotlin.serialization)
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    api(deps.okHttp)
    api(deps.androidx.datastore.preferences.core)
    implementation(deps.bouncycastle.bcprov)
    implementation(deps.moshi)
    ksp(deps.moshi.kotlin.codegen)
    implementation(deps.retrofit)
    // endregion

    // region Firebase
    implementation(platform(deps.firebase.bom))
    implementation(deps.firebase.crashlytics)
    implementation(deps.firebase.perf) {
        exclude(group = "com.google.firebase", module = "protolite-well-known-types")
        exclude(group = "com.google.protobuf", module = "protobuf-javalite")
    }
    // endregion

    // region Tangem
    implementation(tangemDeps.card.android)
    implementation(tangemDeps.card.core)
    // endregion

    // region Core modules
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.configToggles)
    implementation(projects.core.local)
    implementation(projects.core.remote)
    implementation(projects.core.utils)
    // endregion

    // region Tests
    testImplementation(projects.test.core)
    testImplementation(deps.androidx.datastore)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    // endregion
}