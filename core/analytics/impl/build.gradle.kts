plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.core.analytics.impl"
}

dependencies {

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Analytics SDKs
    implementation(deps.amplitude)
    implementation(deps.appsflyer)
    implementation(deps.customerio.analytics)
    implementation(deps.customerio.messaging)
    implementation(platform(deps.firebase.bom))
    implementation(deps.firebase.analytics)
    implementation(deps.firebase.crashlytics)
    implementation(platform(deps.otel.bom))
    implementation(deps.otel.api)
    implementation(deps.otel.sdk.common)
    implementation(deps.otel.sdk.metrics)
    implementation(deps.otel.exporter.otlp) {
        // OTel is compiled against OkHttp 5, whose okhttp-jvm artifact duplicates the OkHttp 4
        // classes already on the classpath; the sender runs on the project-wide OkHttp 4.12.
        exclude(group = "com.squareup.okhttp3", module = "okhttp-jvm")
    }
    implementation(deps.otel.disk.buffering)
    // endregion

    // region AndroidX
    implementation(deps.androidx.core)
    // endregion

    // region Tangem
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)
    // endregion

    // region Core modules
    api(projects.core.analytics)
    // Core shouldn't depend on core, but with utils and logging it's necessary.
    implementation(projects.core.utils)
    // endregion

    // region Domain
    implementation(projects.domain.analytics)
    implementation(projects.domain.appsflyer)
    implementation(projects.domain.settings)
    implementation(projects.domain.wallets.models)
    // endregion

    // region Tests
    testImplementation(projects.test.core)
    // endregion
}