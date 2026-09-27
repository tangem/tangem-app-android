import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(deps.plugins.android.test)
    alias(deps.plugins.kotlin.android)
}

android {
    namespace = "com.tangem.benchmark"
    compileSdk = 36

    defaultConfig {
        // `profileable` — the only way to trace a non-debuggable build — exists since API 29
        minSdk = 29
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        missingDimensionStrategy("services", "google")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        // Mirrors `:app`'s `external`: release-like and non-debuggable, so the numbers are comparable
        // with what users get. `debug` is never benchmarked — its numbers are meaningless.
        create("external") {
            isDebuggable = true
            // A build type other than `debug` gets no signing config by default, and an unsigned test
            // APK fails to install with INSTALL_PARSE_FAILED_NO_CERTIFICATES
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(deps.test.junit)
    implementation(deps.test.junit.android)
    implementation(deps.test.benchmark.macro)
    implementation(deps.test.benchmark.macro.junit4)
}