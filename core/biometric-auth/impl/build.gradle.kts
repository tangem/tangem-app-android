plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.core.biometric.impl"

    // `src/prodDi/` holds production DI bindings for interfaces with a `mocked` counterpart.
    // Wired into every build type EXCEPT `mocked`, which supplies its own bindings from `src/mocked/`.
    buildTypes.configureEach {
        if (name != "mocked") {
            sourceSets.named(name) {
                java.srcDir("src/prodDi/kotlin")
            }
        }
    }
}

dependencies {
    api(projects.core.biometricAuth.api)

    implementation(projects.core.decompose)
    implementation(projects.core.utils)

    implementation(deps.androidx.appCompat)
    implementation(deps.androidx.biometric)
    implementation(deps.androidx.core)

    implementation(deps.kotlin.coroutines)

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}